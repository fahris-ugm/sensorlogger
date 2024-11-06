package id.ac.ugm.fahris.sensorlogger.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtils {
    companion object {
        fun shareZipFile(context: Context, zipUri: Uri) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, zipUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ZIP file"))
        }
        fun compressToZip(context: Context, csvUri: Uri, zipUri: Uri) {
            try {
                context.contentResolver.openOutputStream(zipUri)?.use { zipOut ->
                    ZipOutputStream(BufferedOutputStream(zipOut)).use { zos ->
                        context.contentResolver.openInputStream(csvUri)?.use { csvIn ->
                            val entry = ZipEntry("recording.csv")
                            zos.putNextEntry(entry)
                            csvIn.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Error creating ZIP file", Toast.LENGTH_SHORT).show()
            }
        }
        fun createFileInMediaStore(context: Context, fileName: String, mimeType: String): Uri? {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }

            return context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        }

    }
}