package com.zerax23.stokbarcode.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageSaver @Inject constructor() {

    /**
     * Simpan bitmap ke galeri
     * Support API 29+ (MediaStore) dan API 28- (File)
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String = "barcode_${System.currentTimeMillis()}"
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBitmapApi29Plus(context, bitmap, filename)
        } else {
            saveBitmapLegacy(context, bitmap, filename)
        }
    }

    private fun saveBitmapApi29Plus(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/StokBarcode"
            )
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            uri
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            null
        }
    }

    private fun saveBitmapLegacy(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ),
            "StokBarcode"
        )
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$filename.png")
        return try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Simpan PDF ke folder Downloads
     */
    fun savePdfToDownloads(
        context: Context,
        pdfBytes: ByteArray,
        filename: String = "laporan_${System.currentTimeMillis()}"
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$filename.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/StokBarcode"
                )
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return null

            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(pdfBytes)
                }
                uri
            } catch (e: Exception) {
                null
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
                "StokBarcode"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$filename.pdf")
            try {
                FileOutputStream(file).use { it.write(pdfBytes) }
                Uri.fromFile(file)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Share image via intent
     */
    fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Bagikan Barcode")
        )
    }

    /**
     * Share teks via intent
     */
    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, "Bagikan Laporan")
        )
    }
}
