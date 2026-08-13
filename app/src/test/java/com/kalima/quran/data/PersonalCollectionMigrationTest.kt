package com.kalima.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalCollectionMigrationTest {
    @Test
    fun favoritesScopeMigratesToMyList() {
        assertEquals(StudyScope.Custom, StudyScope.fromPersistedName("Favorites"))
    }

    @Test
    fun favoritesAndMyListAreMergedWithoutLosingWords() {
        assertEquals(
            setOf("favorite-only", "shared", "list-only"),
            mergePersonalCollections(
                legacyFavoriteIds = setOf("favorite-only", "shared"),
                customStudyIds = setOf("shared", "list-only"),
            ),
        )
    }
}
