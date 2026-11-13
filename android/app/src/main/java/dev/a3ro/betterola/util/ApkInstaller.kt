package dev.a3ro.betterola.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads a release APK from GitHub and hands it to the system installer.
 * Android needs "install unknown apps" for this app once; [install] opens that screen when it is missing.
 */
object ApkInstaller {

    suspend fun fetch(ctx: Context, url: String, version: String, onProgress: (Float) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val dest = File(ctx.cacheDir, "better-ola-${version.removePrefix("v")}.apk")
            val ok = runCatching {
                ApkDownloader.download(url, dest) { f, _, _ -> onProgress(f.coerceIn(0f, 1f)) }
            }.getOrDefault(false)
            if (ok) dest else null
        }

    fun install(ctx: Context, file: File): Boolean {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        return runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.isSuccess
    }
}
