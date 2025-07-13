package com.example.irlstudentattentiontracker.mjpegcode

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer

class MjpegInputStream(input: InputStream) {
    private val bis = BufferedInputStream(input, FRAME_MAX_LENGTH)

    companion object {
        private const val SOI_MARKER = 0xFFD8.toShort()
        private const val EOI_MARKER = 0xFFD9.toShort()
        private const val FRAME_MAX_LENGTH = 400_000
    }

    fun readMjpegFrame(): ByteArray? {
        try {
            var headerEnd = false
            val headerBuffer = ByteArray(1024)
            var bytesRead = 0

            // Read headers till we find empty line (\r\n\r\n)
            while (!headerEnd && bytesRead < headerBuffer.size) {
                val b = bis.read()
                if (b == -1) return null
                headerBuffer[bytesRead++] = b.toByte()

                if (bytesRead >= 4 &&
                    headerBuffer[bytesRead - 4] == '\r'.code.toByte() &&
                    headerBuffer[bytesRead - 3] == '\n'.code.toByte() &&
                    headerBuffer[bytesRead - 2] == '\r'.code.toByte() &&
                    headerBuffer[bytesRead - 1] == '\n'.code.toByte()
                ) {
                    headerEnd = true
                }
            }

            // Now read image bytes starting from SOI (FFD8) to EOI (FFD9)
            val imageBuffer = ByteBuffer.allocate(FRAME_MAX_LENGTH)
            var prev = bis.read()
            var curr = bis.read()
            imageBuffer.put(prev.toByte())

            while (!(prev == 0xFF && curr == 0xD9)) {
                imageBuffer.put(curr.toByte())
                prev = curr
                curr = bis.read()
                if (curr == -1) break
            }
            imageBuffer.put(0xD9.toByte()) // EOI marker

            val jpegBytes = ByteArray(imageBuffer.position())
            imageBuffer.flip()
            imageBuffer.get(jpegBytes)

            return jpegBytes
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
