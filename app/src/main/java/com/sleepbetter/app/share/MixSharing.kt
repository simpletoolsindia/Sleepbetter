package com.sleepbetter.app.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sleepbetter.core.mix.Mix
import com.sleepbetter.core.mix.MixCodec
import java.io.File

/**
 * Sends a mix to another phone. The mix travels as a tiny text file (plus the
 * same text in the message), which Quick Share and Bluetooth both accept. On
 * the other phone, opening the file or the link with SleepBetter imports it.
 */
object MixSharing {
    enum class Route { QUICK_SHARE, BLUETOOTH, ANY_APP }

    /** Known receivers: Google's Quick Share, and the system Bluetooth sender. */
    private val quickShare = ComponentName("com.google.android.gms", "com.google.android.gms.nearby.sharing.send.SendActivity")
    private const val BLUETOOTH_PACKAGE = "com.android.bluetooth"

    fun share(context: Context, mix: Mix, route: Route) {
        val text = MixCodec.shareText(mix)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SleepBetter mix: ${mix.name}")
            putExtra(Intent.EXTRA_TEXT, text)
            fileFor(context, mix, text)?.let { uri ->
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(mix.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val direct = when (route) {
            Route.QUICK_SHARE -> Intent(send).setComponent(quickShare)
            Route.BLUETOOTH -> Intent(send).setPackage(BLUETOOTH_PACKAGE)
            Route.ANY_APP -> null
        }
        val chooser = Intent.createChooser(send, "Share \"${mix.name}\"")
        try {
            context.startActivity((direct ?: chooser).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            // This phone names Quick Share or Bluetooth differently: let the user pick.
            context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: SecurityException) {
            context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun fileFor(context: Context, mix: Mix, text: String) = runCatching {
        val dir = File(context.cacheDir, "mixes").apply { mkdirs() }
        val safe = mix.name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifEmpty { "mix" }.take(30)
        val file = File(dir, "SleepBetter mix - $safe.txt")
        file.writeText(text)
        FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    }.getOrNull()

    /** Pulls text out of an incoming intent: a link, shared text, or a received file (read with a size cap). */
    fun textFrom(context: Context, intent: Intent): String? = when (intent.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.streamUri()?.let { readCapped(context, it) }
        Intent.ACTION_VIEW -> {
            val data = intent.data
            when (data?.scheme) {
                "sleepbetter" -> data.toString()
                "content" -> readCapped(context, data)
                else -> null
            }
        }
        else -> null
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): android.net.Uri? = getParcelableExtra(Intent.EXTRA_STREAM)

    private fun readCapped(context: Context, uri: android.net.Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            // A mix file is tiny; never read more than 16 KB from whatever was opened.
            val buf = ByteArray(16 * 1024)
            var total = 0
            while (total < buf.size) {
                val n = input.read(buf, total, buf.size - total)
                if (n <= 0) break
                total += n
            }
            String(buf, 0, total, Charsets.UTF_8)
        }
    }.getOrNull()
}
