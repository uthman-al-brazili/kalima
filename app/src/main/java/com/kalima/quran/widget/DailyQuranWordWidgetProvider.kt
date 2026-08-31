package com.kalima.quran.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.net.toUri
import com.kalima.quran.MainActivity
import com.kalima.quran.R
import com.kalima.quran.background.AsyncBroadcastWork
import com.kalima.quran.data.ProgressStore
import com.kalima.quran.localization.LanguageManager

/**
 * A small, read-only study prompt for the launcher. The widget deliberately
 * never records an answer or introduces a card: its Next button only changes
 * what is displayed, leaving spaced-repetition progress entirely untouched.
 */
class DailyQuranWordWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val applicationContext = context.applicationContext
        AsyncBroadcastWork.run(this, "widget update") {
            appWidgetIds.forEach { appWidgetId ->
                updateWidget(applicationContext, manager, appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        val applicationContext = context.applicationContext
        AsyncBroadcastWork.run(this, "widget enable") { updateAll(applicationContext) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_NEXT) return

        val applicationContext = context.applicationContext
        AsyncBroadcastWork.run(this, "widget next") {
            val manager = AppWidgetManager.getInstance(applicationContext)
            val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: manager.getAppWidgetIds(
                    ComponentName(applicationContext, DailyQuranWordWidgetProvider::class.java),
                )
            val preferences = applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            widgetIds.forEach { appWidgetId ->
                preferences.edit {
                    putInt(
                        sequenceKey(appWidgetId),
                        preferences.getInt(sequenceKey(appWidgetId), 0) + 1,
                    )
                }
                updateWidget(applicationContext, manager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences.edit {
            appWidgetIds.forEach { remove(sequenceKey(it)) }
        }
    }

    private fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, DailyQuranWordWidgetProvider::class.java))
        ids.forEach { appWidgetId -> updateWidget(context, manager, appWidgetId) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val localized = LanguageManager.localizedContext(context)
        val progress = ProgressStore.get(context).progress.value
        val sequence = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getInt(sequenceKey(appWidgetId), 0)
        val word = DailyQuranWordSelector.select(progress, sequence)
        val views = RemoteViews(localized.packageName, R.layout.widget_daily_quran_word).apply {
            setTextViewText(R.id.widget_title, localized.getString(R.string.widget_daily_word_title))
            setTextViewText(R.id.widget_arabic, word.arabic)
            setTextViewText(R.id.widget_transliteration, word.transliteration)
            setTextViewText(R.id.widget_meaning, word.meaning)
            setTextViewText(
                R.id.widget_context,
                localized.getString(R.string.widget_daily_word_context, word.reference),
            )
            setContentDescription(
                R.id.widget_open_area,
                localized.getString(R.string.widget_open_word_description, word.arabic, word.meaning),
            )
            setContentDescription(R.id.widget_next, localized.getString(R.string.widget_next_description))
            setOnClickPendingIntent(R.id.widget_open_area, openWordIntent(localized, appWidgetId, word.id))
            setOnClickPendingIntent(R.id.widget_next, nextIntent(localized, appWidgetId))
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun openWordIntent(context: Context, appWidgetId: Int, wordId: String): PendingIntent {
        val intent = MainActivity.createStudyIntent(context, wordId).apply {
            data = "kalima://study/$wordId".toUri()
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, DailyQuranWordWidgetProvider::class.java).apply {
            action = ACTION_NEXT
            data = "kalima://widget/$appWidgetId/next".toUri()
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val ACTION_NEXT = "com.kalima.quran.widget.action.NEXT"
        const val PREFERENCES = "kalima_daily_word_widget"
        const val SEQUENCE_PREFIX = "sequence_"

        fun sequenceKey(appWidgetId: Int) = "$SEQUENCE_PREFIX$appWidgetId"
    }
}
