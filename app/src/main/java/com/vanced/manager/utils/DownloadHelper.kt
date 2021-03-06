package com.vanced.manager.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.vanced.manager.R
import com.vanced.manager.model.ProgressModel
import com.vanced.manager.utils.AppUtils.sendCloseDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DownloadHelper {

    fun download(url: String, dir: String, child: String, context: Context): Long {
        val request = DownloadManager.Request(url.toUri()).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setTitle(context.getString(R.string.downloading_file, child))
            setDestinationInExternalFilesDir(context, dir, child)
        }

        val downloadManager = context.getSystemService<DownloadManager>()!!
        return downloadManager.enqueue(request)
    }

    val downloadProgress = MutableLiveData<ProgressModel>()

    init {
        downloadProgress.value = ProgressModel()
    }

    suspend fun downloadManager(context: Context) =
        withContext(Dispatchers.IO) {
            val url = "https://github.com/YTVanced/VancedManager/releases/latest/download/manager.apk"
            downloadProgress.value?.currentDownload = PRDownloader.download(url, context.getExternalFilesDir("manager")?.path, "manager.apk")
                .build()
                .setOnProgressListener { progress ->
                    val mProgress = progress.currentBytes * 100 / progress.totalBytes
                    downloadProgress.value?.downloadProgress?.value = mProgress.toInt()
                }
                .setOnCancelListener {
                    downloadProgress.value?.downloadProgress?.value = 0
                }
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        downloadProgress.value?.downloadProgress?.value = 0
                        val apk =
                            File("${context.getExternalFilesDir("manager")?.path}/manager.apk")
                        val uri =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    apk
                                )
                            else
                                Uri.fromFile(apk)

                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, "application/vnd.android.package-archive")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(intent)
                        sendCloseDialog(context)
                    }

                    override fun onError(error: com.downloader.Error?) {
                        Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
                        Log.e("VMUpgrade", error.toString())
                    }

                })
        }
}
