package com.labteto.dshmobile.core.files

import com.labteto.dshmobile.core.wire.dto.WorkspaceFileContent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class WorkspaceDownloadTest {
    private fun chunk(offset: Long, data: ByteArray, version: String = "v1", size: Long = 4, eof: Boolean = false) =
        WorkspaceFileContent("/file", version, size, offset, eof, data)

    @Test fun copiesMultipleChunksWithoutChangingBinaryData() = runTest {
        val sink = ByteArrayOutputStream()
        copyWorkspaceFile(sink) { offset, _ ->
            if (offset == 0L) chunk(0, byteArrayOf(0, -1))
            else chunk(2, byteArrayOf(10, 42), eof = true)
        }
        assertArrayEquals(byteArrayOf(0, -1, 10, 42), sink.toByteArray())
    }

    @Test fun rejectsAChangedFile() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                copyWorkspaceFile(ByteArrayOutputStream()) { offset, _ ->
                    if (offset == 0L) chunk(0, byteArrayOf(1, 2))
                    else chunk(2, byteArrayOf(3, 4), version = "v2", eof = true)
                }
            }
        }
    }

    @Test fun rejectsTruncatedAndStalledReads() = runTest {
        for (part in listOf(chunk(0, byteArrayOf(1), eof = true), chunk(0, byteArrayOf()))) {
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { copyWorkspaceFile(ByteArrayOutputStream()) { _, _ -> part } }
            }
        }
    }

    @Test fun acceptsEmptyFile() = runTest {
        val sink = ByteArrayOutputStream()
        copyWorkspaceFile(sink) { _, _ -> chunk(0, byteArrayOf(), size = 0, eof = true) }
        assertArrayEquals(byteArrayOf(), sink.toByteArray())
    }
}
