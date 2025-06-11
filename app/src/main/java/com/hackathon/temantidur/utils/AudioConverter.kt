package com.hackathon.temantidur.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioConverter(private val context: Context) {

    companion object {
        private const val TAG = "AudioConverter"
        private const val WAV_HEADER_SIZE = 44
    }

    fun convertToWav(inputFile: File): File? {
        if (!inputFile.exists()) {
            Log.e(TAG, "Input file does not exist: ${inputFile.absolutePath}")
            return null
        }

        val outputFile = createWavFile()

        return try {
            when {
                inputFile.name.endsWith(".m4a", ignoreCase = true) ||
                        inputFile.name.endsWith(".mp4", ignoreCase = true) -> {
                    convertM4AToWav(inputFile, outputFile)
                }
                inputFile.name.endsWith(".3gp", ignoreCase = true) -> {
                    convert3GPToWav(inputFile, outputFile)
                }
                inputFile.name.endsWith(".wav", ignoreCase = true) -> {
                    inputFile.copyTo(outputFile, overwrite = true)
                    outputFile
                }
                else -> {
                    Log.w(TAG, "Unknown audio format, attempting generic conversion")
                    convertGenericToWav(inputFile, outputFile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting audio to WAV", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            null
        }
    }

    private fun convertM4AToWav(inputFile: File, outputFile: File): File? {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var outputStream: FileOutputStream? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioFormat: MediaFormat? = null
            var audioTrackIndex = -1

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                Log.e(TAG, "No audio track found in M4A file")
                return null
            }

            val mimeType = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            decoder = MediaCodec.createDecoderByType(mimeType)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            extractor.selectTrack(audioTrackIndex)

            outputStream = FileOutputStream(outputFile)
            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            writeWavHeader(outputStream, 0, sampleRate, channelCount)

            val bufferInfo = MediaCodec.BufferInfo()
            var totalDataSize = 0L
            var isInputEOS = false
            var isOutputEOS = false

            while (!isOutputEOS) {
                if (!isInputEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isInputEOS = true
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                while (outputBufferIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isOutputEOS = true
                    }

                    if (bufferInfo.size > 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)!!
                        val pcmData = ByteArray(bufferInfo.size)
                        outputBuffer.get(pcmData)

                        outputStream.write(pcmData)
                        totalDataSize += pcmData.size
                    }

                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                }
            }

            outputStream.close()
            updateWavHeader(outputFile, totalDataSize)

            Log.d(TAG, "Successfully converted M4A to WAV: ${outputFile.absolutePath}, size: ${outputFile.length()}")
            return outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Error converting M4A to WAV", e)
            outputStream?.close()
            if (outputFile.exists()) outputFile.delete()
            return null
        } finally {
            try {
                extractor?.release()
                decoder?.stop()
                decoder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    private fun convert3GPToWav(inputFile: File, outputFile: File): File? {
        return try {
            val inputStream = FileInputStream(inputFile)
            val outputStream = FileOutputStream(outputFile)

            val sampleRate = 8000
            val channelCount = 1

            writeWavHeader(outputStream, inputFile.length(), sampleRate, channelCount)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytes = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }

            inputStream.close()
            outputStream.close()

            updateWavHeader(outputFile, totalBytes)

            Log.d(TAG, "Successfully converted 3GP to WAV: ${outputFile.absolutePath}")
            outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Error converting 3GP to WAV", e)
            null
        }
    }

    private fun convertGenericToWav(inputFile: File, outputFile: File): File? {
        return try {
            val sampleRate = 44100
            val channelCount = 2

            val outputStream = FileOutputStream(outputFile)
            writeWavHeader(outputStream, inputFile.length(), sampleRate, channelCount)

            val inputStream = FileInputStream(inputFile)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            updateWavHeader(outputFile, inputFile.length())

            Log.d(TAG, "Generic conversion to WAV completed")
            outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Error in generic conversion", e)
            null
        }
    }

    private fun writeWavHeader(
        outputStream: FileOutputStream,
        dataSize: Long,
        sampleRate: Int,
        channelCount: Int
    ) {
        val bitsPerSample = 16
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8
        val totalSize = dataSize + WAV_HEADER_SIZE - 8

        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(totalSize.toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(channelCount.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray())
            putInt(dataSize.toInt())
        }

        outputStream.write(header.array())
    }

    private fun updateWavHeader(file: File, dataSize: Long) {
        try {
            val randomAccessFile = java.io.RandomAccessFile(file, "rw")

            randomAccessFile.seek(4)
            randomAccessFile.writeInt(Integer.reverseBytes((dataSize + WAV_HEADER_SIZE - 8).toInt()))

            randomAccessFile.seek(40)
            randomAccessFile.writeInt(Integer.reverseBytes(dataSize.toInt()))

            randomAccessFile.close()

        } catch (e: IOException) {
            Log.e(TAG, "Error updating WAV header", e)
        }
    }

    private fun createWavFile(): File {
        val audioDir = File(context.cacheDir, "audio_wav")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        return File(audioDir, "converted_audio_$timestamp.wav")
    }
}