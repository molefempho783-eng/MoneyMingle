package com.iie.group8_prog7313_poe_pt_2.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Persists receipt images under app files dir (MM-11 / MM-20). Stores absolute path on [com.iie.group8_prog7313_poe_pt_2.model.entity.Expense.receiptImagePath].
 */
object ReceiptFileHelper {

    private const val TAG = "ReceiptFileHelper"
    private const val RECEIPTS_SUBDIR = "receipts"

    // filesDir is app-private so receipts are not accessible to other apps or the public gallery
    fun receiptsDir(context: Context): File {
        val dir = File(context.filesDir, RECEIPTS_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir // (Android Developers, 2026)
    }

    // UUID in the filename prevents collisions when multiple photos are taken in the same second
    fun createCameraDestinationFile(context: Context): File {
        val name = "receipt_${UUID.randomUUID()}.jpg"
        return File(receiptsDir(context), name)
    }

    /**
     * Copy content from a gallery [contentUri] into app storage; returns absolute path or null.
     */
    fun copyContentUriToReceiptFile(context: Context, contentUri: Uri): String? {
        return try {
            val dest = createCameraDestinationFile(context)
            context.contentResolver.openInputStream(contentUri)?.use { input -> // (Android Developers, 2026)
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            dest.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "copyContentUriToReceiptFile failed", e)
            null
        }
    }

    fun deleteFileIfExists(absolutePath: String?) {
        if (absolutePath.isNullOrBlank()) return
        runCatching {
            val f = File(absolutePath)
            if (f.exists()) f.delete()
        }
    }

    fun uriPointsToExistingFile(absolutePath: String?): Boolean {
        if (absolutePath.isNullOrBlank()) return false
        return File(absolutePath).exists()
    }
}

/*
  Reference list :
  - Android Developers, 2026. App-specific storage on Android [online]. Available at:
    <https://developer.android.com/training/data-storage/app-specific> [Accessed 20 April 2026].
 */
