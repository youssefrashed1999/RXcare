package com.example.rxcare.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object UriUtils {
    
    /**
     * Converts a content URI to a temporary File that can be uploaded.
     * This handles both content URIs (from gallery/camera) and file URIs.
     */
    fun uriToFile(context: Context, uri: String): File? {
        return try {
            val uriObj = Uri.parse(uri)
            
            when {
                uriObj.scheme == "content" -> {
                    // Handle content URI (from gallery, camera, etc.)
                    copyContentUriToFile(context, uriObj)
                }
                uriObj.scheme == "file" -> {
                    // Handle file URI directly
                    File(uriObj.path ?: return null)
                }
                else -> {
                    // Try to handle as file path
                    File(uri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Copies content from a content URI to a temporary file.
     */
    private fun copyContentUriToFile(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            
            if (inputStream == null) {
                return null
            }
            
            // Create a temporary file
            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            
            // Copy the content to the temporary file
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Cleans up temporary image files in the cache directory.
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles { file ->
                file.name.startsWith("temp_image_") && file.name.endsWith(".jpg")
            }?.forEach { file ->
                if (file.delete()) {
                    // Successfully deleted
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Gets the file name from a content URI if available.
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Gets the MIME type of a content URI.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.getType(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
