package com.labteto.dshmobile.core.files

import com.labteto.dshmobile.core.wire.dto.WorkspaceFileContent
import java.io.OutputStream

/** Copy bounded reads to a destination while rejecting a file changed during transfer. */
suspend fun copyWorkspaceFile(
    sink: OutputStream,
    read: suspend (offset: Long, length: Int) -> WorkspaceFileContent,
) {
    var offset = 0L
    var version: String? = null
    var expectedSize: Long? = null
    while (true) {
        val part = read(offset, 256 * 1024)
        require(part.offset == offset) { "Unexpected file offset" }
        if (version == null) {
            version = part.version
            expectedSize = part.bytes
        } else {
            require(part.version == version && (expectedSize == null || part.bytes == null || part.bytes == expectedSize)) {
                "File changed during download"
            }
        }
        require(part.data.size <= 256 * 1024) { "File read exceeded requested size" }
        require(part.data.isNotEmpty() || part.eof) { "File read made no progress" }
        sink.write(part.data)
        offset += part.data.size
        if (part.eof) {
            require(expectedSize == null || offset == expectedSize) { "Incomplete file download" }
            break
        }
        require(expectedSize == null || offset < expectedSize) { "Unexpected end of file" }
    }
}
