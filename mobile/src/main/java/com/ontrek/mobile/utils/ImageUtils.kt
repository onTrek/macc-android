package com.ontrek.mobile.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Utility functions for handling image orientation issues,
 * particularly for images captured from physical device cameras
 * that embed EXIF rotation metadata.
 */
object ImageUtils {

    /**
     * Reads the EXIF orientation from an image URI and returns the rotation degrees.
     *
     * @param context The application context
     * @param uri The URI of the image to check
     * @return The rotation degrees needed (0, 90, 180, or 270)
     */
    fun getRotationFromExif(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Rotates a bitmap by the specified degrees.
     *
     * @param bitmap The bitmap to rotate
     * @param degrees The rotation angle in degrees
     * @return The rotated bitmap, or the original if rotation is 0
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Corrects the orientation of an image based on its EXIF data.
     * Creates a new temporary file with the corrected image if rotation is needed.
     *
     * @param context The application context
     * @param uri The URI of the image to correct
     * @return A URI to the corrected image (may be the same as input if no correction needed)
     */
    fun correctImageOrientation(context: Context, uri: Uri): Uri {
        val rotation = getRotationFromExif(context, uri)

        // No rotation needed
        if (rotation == 0) return uri

        return try {
            // Decode the bitmap
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return uri

            // Rotate the bitmap
            val rotatedBitmap = rotateBitmap(originalBitmap, rotation)

            // Save to a new temp file
            val correctedFile = File.createTempFile("corrected_", ".jpg", context.cacheDir)
            FileOutputStream(correctedFile).use { outputStream ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }

            // Recycle bitmaps if they're different
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            // Return URI for the corrected file
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                correctedFile
            )
        } catch (e: Exception) {
            // If correction fails, return the original URI
            uri
        }
    }
}
