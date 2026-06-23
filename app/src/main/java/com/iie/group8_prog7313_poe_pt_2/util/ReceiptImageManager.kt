package com.iie.group8_prog7313_poe_pt_2.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Manages camera-captured receipt images stored in external app-private Pictures directory
object ReceiptImageManager {
    // Appended to package name to match the FileProvider authority declared in AndroidManifest.xml
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * Creates an empty JPEG file in the app's private Pictures directory and
     * returns a Pair of the File (for storing its path in Room) and the
     * content:// URI (to pass to the camera intent as MediaStore.EXTRA_OUTPUT).
     */
    fun createImageFile(context: Context): Pair<File, Uri> {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("RECEIPT_${timestamp}_", ".jpg", storageDir)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            imageFile
        )
        return Pair(imageFile, uri)
    }

    /**
     * Deletes the receipt image file from disk.
     * Safe to call with null or a missing path — does nothing in those cases.
     */
    fun deleteReceiptImage(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.exists()) file.delete()
    }
}