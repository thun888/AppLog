package top.hzchu.applog.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object ApkHelper {

    /**
     * 提取 APK 到下载目录
     */
    fun extractApk(
        context: Context,
        packageName: String,
        appName: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val pkgInfo = pm.getPackageInfo(packageName, 0)
            val versionName = pkgInfo.versionName ?: "unknown"
            val sourceFile = File(appInfo.publicSourceDir)
            val totalSize = sourceFile.length()
            val fileName = "${appName}_${packageName}_v${versionName}.apk"

            val inputStream = sourceFile.inputStream()
            
            val outputStream: OutputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Failed to create MediaStore entry"))
                resolver.openOutputStream(uri) ?: return Result.failure(Exception("Failed to open output stream"))
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val destFile = File(downloadDir, fileName)
                FileOutputStream(destFile)
            }

            outputStream.use { out ->
                inputStream.use { ins ->
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    var read = ins.read(buffer)
                    while (read >= 0) {
                        out.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalSize > 0) {
                            onProgress(bytesCopied.toFloat() / totalSize)
                        }
                        read = ins.read(buffer)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 分享 APK
     */
    fun shareApk(context: Context, packageName: String, appName: String) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val pkgInfo = pm.getPackageInfo(packageName, 0)
            val versionName = pkgInfo.versionName ?: "unknown"
            val sourceFile = File(appInfo.publicSourceDir)
            
            // 复制到缓存目录进行分享
            val sharesDir = File(context.cacheDir, "shares")
            if (!sharesDir.exists()) sharesDir.mkdirs()
            val cacheFile = File(sharesDir, "${appName}_v${versionName}.apk")
            sourceFile.copyTo(cacheFile, overwrite = true)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, appName))
        } catch (e: Exception) {
            // handle error
        }
    }
}
