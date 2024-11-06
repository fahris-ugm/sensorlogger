package id.ac.ugm.fahris.sensorlogger.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
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
                            val entry = ZipEntry(csvUri.lastPathSegment)
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
        fun zipDirectory(context: Context, directoryUri: Uri, zipFile: File) {
            try {
                // Open a ZipOutputStream to write the ZIP file
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    // Get the directory from the Uri
                    val directory = File(directoryUri.path ?: return)

                    // Check if the directory exists and is a directory
                    if (directory.exists() && directory.isDirectory) {
                        // Recursively add files to the ZIP
                        addFilesToZip(directory, directory, zos)
                    } else {
                        throw FileNotFoundException("The provided URI does not point to a valid directory.")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        private fun addFilesToZip(rootDir: File, sourceFile: File, zos: ZipOutputStream) {
            if (sourceFile.isDirectory) {
                // If the file is a directory, recursively add its contents
                sourceFile.listFiles()?.forEach { file ->
                    addFilesToZip(rootDir, file, zos)
                }
            } else {
                // If the file is a regular file, add it to the ZIP
                FileInputStream(sourceFile).use { fis ->
                    val relativePath = sourceFile.absolutePath.substring(rootDir.absolutePath.length + 1)
                    val zipEntry = ZipEntry(relativePath)
                    zos.putNextEntry(zipEntry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
        fun zipDirectoryAndSaveToMediaStore(context: Context, directoryName: String, zipFileName: String): Uri? {
            // Create the ZIP file in MediaStore
            val zipUri = createFileInMediaStore(context, zipFileName, "application/zip")
            if (zipUri == null) {
                // Handle error if the file couldn't be created
                return zipUri
            }

            try {
                context.contentResolver.openOutputStream(zipUri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$directoryName"
                        // Query the MediaStore for all files in the specified directory
                        val collection = MediaStore.Files.getContentUri("external")
                        val projection = arrayOf(
                            MediaStore.Files.FileColumns.DISPLAY_NAME,
                            MediaStore.Files.FileColumns._ID
                        )
                        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
                        val selectionArgs = arrayOf("%$relativePath%")

                        val cursor = context.contentResolver.query(
                            collection,
                            projection,
                            selection,
                            selectionArgs,
                            null
                        )

                        if (cursor != null) {
                            while (cursor.moveToNext()) {
                                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                                val fileUri = Uri.withAppendedPath(collection, id.toString())

                                // Add the file to the ZIP
                                context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                    val zipEntry = ZipEntry(displayName)
                                    zos.putNextEntry(zipEntry)
                                    inputStream.copyTo(zos)
                                    zos.closeEntry()
                                }
                            }
                            cursor.close()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
            return zipUri
        }
        fun createFileInMediaStore(context: Context, fileName: String, mimeType: String): Uri? {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }

            return context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        }
        fun createFileInSubDirectory(context: Context, fileName: String, mimeType: String, subDirectory: String): Uri? {
            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$subDirectory"

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            return context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        }
        fun deleteSubDirectoryInMediaStore(context: Context, subDirectoryName: String) {
            // Define the relative path of the sub-directory
            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$subDirectoryName"

            // Query all files in the specified sub-directory
            val collection = MediaStore.Files.getContentUri("external")
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(relativePath)

            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(collection, null, selection, selectionArgs, null)

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // Get the ID of the file to delete
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val fileUri = MediaStore.Files.getContentUri("external", id)

                    // Delete the file
                    contentResolver.delete(fileUri, null, null)
                }
                cursor.close()
            }
        }
    }
}