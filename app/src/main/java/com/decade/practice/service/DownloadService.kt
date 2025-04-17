package com.decade.practice.service

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import com.decade.practice.components.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val FILENAME = "FILENAME"

@AndroidEntryPoint
class DownloadService : Service() {
    @Inject
    lateinit var provider: ImageProvider
    private val serviceScope = MainScope()
    override fun onBind(intent: Intent): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        val filename = intent.getStringExtra(FILENAME) ?: return START_NOT_STICKY
        downloadAndSave(filename, startId)
        return START_REDELIVER_INTENT
    }

    private fun downloadAndSave(filename: String, startId: Int) = serviceScope.launch {
        if (filename.isEmpty()) {
            Toast.makeText(this@DownloadService, "Not supported", Toast.LENGTH_LONG).show()
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            Toast.makeText(this@DownloadService, "Saved image failed", Toast.LENGTH_LONG).show()
            return@launch
        }
        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                provider.load(filename).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        Toast.makeText(this@DownloadService, "Saved image", Toast.LENGTH_LONG).show()
        stopSelf(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}