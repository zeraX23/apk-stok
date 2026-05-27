package com.zerax23.stokbarcode.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeGenerator @Inject constructor() {

    /**
     * Generate CODE_128 barcode bitmap
     * Kompatibel dengan thermal printer 203 DPI
     */
    fun generateCode128Bitmap(
        content: String,
        widthPx: Int = 800,
        heightPx: Int = 300
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 10,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.CODE_128,
                widthPx,
                heightPx,
                hints
            )
            bitMatrixToBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate QR Code bitmap
     * Error correction Level M (15%)
     * Quiet zone 4 modules (standar ISO 18004)
     */
    fun generateQrCodeBitmap(
        content: String,
        sizePx: Int = 600
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 4,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            bitMatrixToBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate barcode khusus untuk print
     * Background putih solid, foreground hitam solid
     * Quiet zone minimal 10px semua sisi
     */
    fun generateForPrint(
        content: String,
        format: BarcodeFormat = BarcodeFormat.CODE_128,
        widthPx: Int = 800,
        heightPx: Int = 300
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 10,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = MultiFormatWriter().encode(
                content,
                format,
                widthPx,
                heightPx,
                hints
            )

            // Buat bitmap dengan background putih solid
            val bitmap = Bitmap.createBitmap(
                widthPx,
                heightPx,
                Bitmap.Config.ARGB_8888
            )

            for (x in 0 until widthPx) {
                for (y in 0 until heightPx) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate label lengkap siap print:
     * [Nama Produk]
     * [Barcode Image]
     * [Serial Number]
     * [Harga]
     */
    fun generateLabelBitmap(
        productName: String,
        serialNumber: String,
        price: String? = null,
        barcodeContent: String,
        format: BarcodeFormat = BarcodeFormat.CODE_128,
        labelWidthPx: Int = 800,
        labelHeightPx: Int = 400
    ): Bitmap? {
        return try {
            val label = Bitmap.createBitmap(
                labelWidthPx,
                labelHeightPx,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(label)

            // Background putih
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            // 1. Nama produk di atas (bold, center)
            paint.textSize = 36f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                productName,
                labelWidthPx / 2f,
                50f,
                paint
            )

            // 2. Barcode di tengah
            val barcodeHeight = labelHeightPx / 2
            val barcodeBitmap = generateForPrint(
                content = barcodeContent,
                format = format,
                widthPx = labelWidthPx - 40,
                heightPx = barcodeHeight
            )
            barcodeBitmap?.let {
                canvas.drawBitmap(it, 20f, 70f, null)
            }

            // 3. Serial number
            paint.textSize = 28f
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                serialNumber,
                labelWidthPx / 2f,
                70f + barcodeHeight + 40f,
                paint
            )

            // 4. Harga (jika ada)
            price?.let {
                paint.textSize = 28f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    it,
                    labelWidthPx / 2f,
                    70f + barcodeHeight + 80f,
                    paint
                )
            }

            label
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert BitMatrix ke Bitmap
     * Hitam murni di atas putih murni
     */
    private fun bitMatrixToBitmap(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(
            width, height,
            Bitmap.Config.ARGB_8888
        )
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }
        return bitmap
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
