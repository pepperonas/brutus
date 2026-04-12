package com.pepperonas.brutus.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object QrGenerator {

    fun generateData(): String = "brutus:${UUID.randomUUID()}"

    fun generateBitmap(data: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Saves the QR code as a high-resolution PNG to the public Pictures/Brutus folder.
     * Returns the content Uri on success, or null on failure.
     */
    fun savePng(context: Context, data: String, size: Int = 1024): Uri? {
        val bitmap = generateBitmap(data, size)
        val filename = "brutus-qr-${data.takeLast(8)}.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savePngMediaStore(context, bitmap, filename)
        } else {
            savePngLegacy(bitmap, filename)
        }
    }

    private fun savePngMediaStore(context: Context, bitmap: Bitmap, filename: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Brutus")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return null
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }
    }

    /**
     * Writes the QR as a PNG into the app's cache and launches an ACTION_SEND chooser
     * so the user can share it via Messenger, Mail, Bluetooth, etc.
     */
    fun shareQr(context: Context, data: String, size: Int = 1024): Boolean {
        return try {
            val bitmap = generateBitmap(data, size)
            val cacheDir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            val file = File(cacheDir, "brutus-qr-${data.takeLast(8)}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Brutus QR-Code")
                putExtra(Intent.EXTRA_TEXT, "Mein Brutus QR-Code zum Wecker-Ausschalten.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "QR-Code teilen").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun savePngLegacy(bitmap: Bitmap, filename: String): Uri? {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Brutus"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }
}
