package com.sleepbetter.core.mix

import com.sleepbetter.core.audio.SoundId
import java.util.Base64
import kotlin.math.roundToInt

/**
 * Turns a mix into a short, link-safe code and back, so it can travel by
 * Quick Share, Bluetooth, a message or the clipboard.
 *
 * Link: `sleepbetter://mix/1.<base64url payload>`, where the payload is
 * `name|RN:0,-35;TH:50,-45` (two-letter sound code, then x and y in
 * hundredths). Decoding never trusts its input: unknown sounds are skipped,
 * places are clamped to the stage, and anything malformed returns null.
 */
object MixCodec {
    const val LINK_PREFIX = "sleepbetter://mix/"
    private const val VERSION = "1."
    private const val MAX_CODE = 600

    private val byCode: Map<String, SoundId> = SoundId.entries.associateBy { it.code }
    private val linkPattern = Regex("""sleepbetter://mix/([A-Za-z0-9_.\-]{4,600})""")
    private val barePattern = Regex("""(?<![A-Za-z0-9_\-])(1\.[A-Za-z0-9_\-]{4,600})""")

    fun encode(mix: Mix): String {
        val layers = mix.layers.joinToString(";") { "${it.sound.code}:${(it.x * 100).roundToInt()},${(it.y * 100).roundToInt()}" }
        val payload = "${Mix.cleanName(mix.name)}|$layers"
        return VERSION + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(Charsets.UTF_8))
    }

    fun link(mix: Mix): String = LINK_PREFIX + encode(mix)

    /** What we send: a friendly line plus the link (which the receiving app also finds inside a file). */
    fun shareText(mix: Mix): String =
        "Try my SleepBetter mix \"${mix.name}\" (${mix.layers.joinToString(", ") { it.sound.label.lowercase() }}).\n" +
            "Open with SleepBetter:\n${link(mix)}"

    fun decode(code: String): Mix? {
        val c = code.trim()
        if (!c.startsWith(VERSION) || c.length > MAX_CODE) return null
        val payload = runCatching { String(Base64.getUrlDecoder().decode(c.removePrefix(VERSION)), Charsets.UTF_8) }.getOrNull() ?: return null
        val bar = payload.indexOf('|')
        if (bar < 0) return null
        val layers = payload.substring(bar + 1).split(';').mapNotNull { part ->
            val colon = part.indexOf(':')
            if (colon != 2) return@mapNotNull null
            val sound = byCode[part.substring(0, 2)] ?: return@mapNotNull null
            val xy = part.substring(3).split(',')
            if (xy.size != 2) return@mapNotNull null
            val x = xy[0].toIntOrNull() ?: return@mapNotNull null
            val y = xy[1].toIntOrNull() ?: return@mapNotNull null
            MixLayer(sound, x.coerceIn(-100, 100) / 100f, y.coerceIn(-100, 100) / 100f)
        }
        if (layers.isEmpty()) return null
        return Mix.of(payload.substring(0, bar).ifBlank { "Shared mix" }, layers)
    }

    /** Finds a mix anywhere in a message or file: the full link, or a bare code. */
    fun findIn(text: String): Mix? {
        val sample = text.take(16_000)
        linkPattern.find(sample)?.let { m -> decode(m.groupValues[1])?.let { return it } }
        barePattern.findAll(sample).forEach { m -> decode(m.groupValues[1])?.let { return it } }
        return null
    }
}
