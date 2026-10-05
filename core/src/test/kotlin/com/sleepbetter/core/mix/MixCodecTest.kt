package com.sleepbetter.core.mix

import com.sleepbetter.core.audio.SoundId
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MixCodecTest {
    private val storm = Mix.of(
        "Cozy storm",
        listOf(MixLayer(SoundId.DOWNPOUR, 0f, -0.2f), MixLayer(SoundId.THUNDER, 0.45f, -0.5f)),
    )

    @Test
    fun roundTripKeepsNameSoundsAndPlaces() {
        val back = MixCodec.decode(MixCodec.encode(storm))
        assertNotNull(back)
        assertEquals("Cozy storm", back.name)
        assertEquals(storm.sounds, back.sounds)
        val thunder = back.layers.first { it.sound == SoundId.THUNDER }
        assertEquals(0.45f, thunder.x, 0.01f)
        assertEquals(-0.5f, thunder.y, 0.01f)
    }

    @Test
    fun everyTemplateSurvivesARoundTrip() {
        for (t in MixTemplates.all) {
            val back = MixCodec.decode(MixCodec.encode(t.mix))
            assertNotNull(back, t.mix.name)
            assertEquals(t.mix.sounds, back.sounds, t.mix.name)
        }
    }

    @Test
    fun findsTheLinkInsideAMessage() {
        val message = "Hey! " + MixCodec.shareText(storm) + "\nSleep well"
        assertEquals("Cozy storm", MixCodec.findIn(message)?.name)
    }

    @Test
    fun findsABareCode() {
        val code = MixCodec.encode(storm)
        assertEquals(storm.sounds, MixCodec.findIn("code: $code")?.sounds)
    }

    @Test
    fun unicodeNamesWork() {
        val mix = Mix.of("Mazhai இரவு 🌧", listOf(MixLayer(SoundId.RAIN)))
        assertEquals("Mazhai இரவு 🌧", MixCodec.decode(MixCodec.encode(mix))?.name)
    }

    @Test
    fun rejectsJunk() {
        assertNull(MixCodec.decode(""))
        assertNull(MixCodec.decode("hello"))
        assertNull(MixCodec.decode("1.!!!notbase64"))
        assertNull(MixCodec.decode("1." + "A".repeat(2000)))
        assertNull(MixCodec.findIn("nothing to see here"))
        // Valid base64 but no sounds we know.
        val noSounds = "1." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("Name|ZZ:1,2;QQ:3,4".toByteArray())
        assertNull(MixCodec.decode(noSounds))
    }

    @Test
    fun hostileValuesAreCleaned() {
        val payload = "Evil\u0000name\nwith controls" + "x".repeat(100) + "|RN:9999,-9999;RN:0,0;TH:abc,1"
        val code = "1." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val mix = MixCodec.decode(code)
        assertNotNull(mix)
        assertTrue(mix.name.length <= Mix.MAX_NAME)
        assertTrue(mix.name.none { it.isISOControl() || it == '|' })
        assertEquals(1, mix.layers.size, "duplicates and bad numbers are dropped")
        val l = mix.layers.single()
        assertTrue(sqrt(l.x * l.x + l.y * l.y) <= 1.0001f, "places stay on the stage")
    }

    @Test
    fun templatesCoverTheRequestedMixes() {
        val names = MixTemplates.all.map { it.mix.name }
        listOf("Heavy rain", "Heavy rain and thunder", "Forest rain and thunder", "Forest and thunder", "Rain drops only")
            .forEach { assertTrue(it in names, "missing template $it") }
        assertTrue(MixTemplates.featured.isNotEmpty())
    }
}
