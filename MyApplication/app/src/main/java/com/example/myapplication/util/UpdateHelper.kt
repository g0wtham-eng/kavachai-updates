package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object UpdateHelper {
    private const val TAG = "UpdateHelper"
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/g0wtham-eng/kavachai-updates/main/manifest.json"

    private val client = OkHttpClient()

    suspend fun checkForUpdates(context: Context): Boolean {
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "No network – cannot check for updates")
            return false
        }

        val manifest = downloadManifest() ?: return false
        val currentVersion = BuildConfig.VERSION_CODE
        Log.d(TAG, "Current version: $currentVersion, Latest version: ${manifest.latestVersionCode}")
        if (manifest.latestVersionCode > currentVersion) {
            Log.i(TAG, "New version ${manifest.latestVersionCode} available")
            downloadAndInstallApk(context, manifest.apkUrl)
            return true
        } else {
            Log.i(TAG, "App is up‑to‑date")
            return false
        }
    }

    private suspend fun downloadManifest(): Manifest? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(MANIFEST_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch manifest: ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val version = Regex("\"latestVersionCode\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toInt() ?: 0
                val url = Regex("\"apkUrl\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                val checksum = Regex("\"checksum\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1) ?: ""
                Manifest(version, url, checksum)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading manifest", e)
            null
        }
    }

    private suspend fun downloadAndInstallApk(context: Context, apkUrl: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading APK from: $apkUrl")
            val request = Request.Builder().url(apkUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "APK download failed: ${response.code}")
                    return@withContext
                }
                val apkDir = File(context.cacheDir, "apk")
                if (!apkDir.exists()) {
                    apkDir.mkdirs()
                }
                val apkFile = File(apkDir, "update.apk")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(apkFile).use { output -> input.copyTo(output) }
                }
                Log.i(TAG, "APK saved to ${apkFile.absolutePath}")

                // Launch Android installer (user must confirm)
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    data class Manifest(
        val latestVersionCode: Int,
        val apkUrl: String,
        val checksum: String
    )
}
