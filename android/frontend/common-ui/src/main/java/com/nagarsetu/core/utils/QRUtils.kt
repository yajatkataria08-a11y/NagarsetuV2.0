package com.nagarsetu.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QRUtils {
    fun generateQrBitmap(data: String, size: Int = 512): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
            Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
