package com.kalima.quran.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProgressBackupInputTest {
    @Test
    fun `reads backup at byte limit`() {
        val backup = "12345678"

        assertEquals(
            backup,
            ProgressBackupCodec.read(ByteArrayInputStream(backup.toByteArray()), maxBytes = 8),
        )
    }

    @Test
    fun `rejects backup over byte limit`() {
        assertThrows(ProgressBackupValidationException::class.java) {
            ProgressBackupCodec.read(ByteArrayInputStream(ByteArray(9)), maxBytes = 8)
        }
    }

    @Test
    fun `applies limit to encoded bytes rather than characters`() {
        val backup = "قرآن"

        assertThrows(ProgressBackupValidationException::class.java) {
            ProgressBackupCodec.read(ByteArrayInputStream(backup.toByteArray()), maxBytes = 7)
        }
    }
}
