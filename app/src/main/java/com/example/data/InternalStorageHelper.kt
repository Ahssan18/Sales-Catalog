package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object InternalStorageHelper {

    private const val IMAGES_DIR = "snack_images"

    fun getImagesDir(context: Context): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Creates a temporary file in cache for Camera capture via FileProvider.
     */
    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val cacheDir = File(context.cacheDir, "camera_temp")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = File(cacheDir, "temp_cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Pair(uri, file)
    }

    /**
     * Saves an image from Uri into internal storage under snack_images/.
     * Returns ONLY the relative filename (e.g. "img_123.jpg") so it can be stored in Room.
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val rotatedBitmap = rotateBitmapIfRequired(context, sourceUri, originalBitmap)
            val scaledBitmap = scaleBitmapDown(rotatedBitmap, 1280)

            val fileName = "img_${UUID.randomUUID()}.jpg"
            val targetFile = File(getImagesDir(context), fileName)

            FileOutputStream(targetFile).use { outStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, outStream)
            }

            fileName // Store relative filename only
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resolves a stored image path:
     * - "res:sample_..." -> resource
     * - relative filename "img_....jpg" -> File in filesDir/snack_images/
     * - legacy absolute path -> File
     */
    fun resolveImageFile(context: Context, imagePath: String?): File? {
        if (imagePath.isNullOrBlank() || imagePath.startsWith("res:")) return null
        val file = if (imagePath.startsWith("/")) {
            File(imagePath)
        } else {
            File(getImagesDir(context), imagePath)
        }
        return if (file.exists()) file else null
    }

    /**
     * Deletes an image file from internal storage if it exists.
     */
    fun deleteInternalImage(context: Context, imagePath: String?) {
        if (imagePath.isNullOrBlank() || imagePath.startsWith("res:")) return
        try {
            val file = resolveImageFile(context, imagePath)
            file?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clean up unreferenced files in snack_images
     */
    fun cleanupOrphanedImages(context: Context, activeImageFilenames: Set<String>) {
        try {
            val dir = getImagesDir(context)
            dir.listFiles()?.forEach { file ->
                if (!activeImageFilenames.contains(file.name)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun rotateBitmapIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
