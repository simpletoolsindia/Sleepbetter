package com.sleepbetter.app.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.sleepbetter.core.audio.Recording
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.audio.StereoClip
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

/**
 * The real recordings bundled in assets/sounds: a seamless loop per sound
 * ("RN.ogg" by its share code) and thunder strikes in sounds/thunder/.
 * Decoding happens off the audio thread, once per sound, and the result is
 * kept for the rest of the session. Sounds without a recording keep their
 * generated version.
 */
class RecordingLibrary(private val context: Context) {
    private val loops: Set<String> = runCatching { context.assets.list("sounds")?.toSet() }.getOrNull() ?: emptySet()
    private val strikes: List<String> = runCatching { context.assets.list("sounds/thunder")?.sorted() }.getOrNull() ?: emptyList()
    private val cache = ConcurrentHashMap<SoundId, Recording>()

    fun has(id: SoundId): Boolean = when {
        id.alwaysGenerated -> false
        id == SoundId.THUNDER -> strikes.isNotEmpty()
        else -> "${id.code}.ogg" in loops
    }

    /** Decodes (or returns the cached) recording for [id]; null if there is none or it cannot be read. Blocking. */
    fun load(id: SoundId): Recording? {
        if (!has(id)) return null
        cache[id]?.let { return it }
        val rec = runCatching {
            if (id == SoundId.THUNDER) {
                Recording.Strikes(strikes.mapNotNull { decode("sounds/thunder/$it") })
            } else {
                decode("sounds/${id.code}.ogg")?.let { Recording.Loop(it) }
            }
        }.onFailure { Log.w(TAG, "Could not load the recording for $id", it) }.getOrNull()
        rec?.let { cache[id] = it }
        return rec
    }

    /** Decodes an asset to 16-bit stereo PCM with the platform decoder. */
    private fun decode(path: String): StereoClip? {
        val fd = context.assets.openFd(path)
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()
            var out = ShortArray(rate * 2 * 30)
            var size = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        val read = extractor.readSampleData(buf, 0)
                        if (read < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, read, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex >= 0 -> {
                        val shorts = codec.getOutputBuffer(outIndex)!!.order(ByteOrder.nativeOrder()).asShortBuffer()
                        shorts.position(info.offset / 2)
                        shorts.limit((info.offset + info.size) / 2)
                        val n = shorts.remaining()
                        val stereo = if (channels == 1) n * 2 else n / channels * 2
                        if (size + stereo > out.size) out = out.copyOf(maxOf(out.size * 2, size + stereo))
                        if (channels == 2) {
                            shorts.get(out, size, n)
                        } else {
                            // Mono → both sides; more than two channels → keep the first two.
                            val tmp = ShortArray(n).also { shorts.get(it) }
                            if (channels == 1) for (i in 0 until n) { out[size + 2 * i] = tmp[i]; out[size + 2 * i + 1] = tmp[i] }
                            else for (f in 0 until n / channels) { out[size + 2 * f] = tmp[f * channels]; out[size + 2 * f + 1] = tmp[f * channels + 1] }
                        }
                        size += stereo
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            codec.stop()
            codec.release()
            return if (size == 0) null else StereoClip(out.copyOf(size), rate)
        } finally {
            extractor.release()
            fd.close()
        }
    }

    private companion object {
        const val TAG = "RecordingLibrary"
    }
}
