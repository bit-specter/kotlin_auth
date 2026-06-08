package cloud.meis.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cloud.meis.MainActivity
import cloud.meis.R
import cloud.meis.data.local.database.AppDatabase
import cloud.meis.data.local.entity.ServerEntity
import cloud.meis.data.model.PingResult
import cloud.meis.data.repository.ServerRepository
import cloud.meis.network.AutoPinger
import kotlinx.coroutines.withTimeoutOrNull

class PingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val repository by lazy {
        ServerRepository(AppDatabase.getInstance(applicationContext).serverDao())
    }
    private val autoPinger = AutoPinger()

    override suspend fun doWork(): Result {
        return runCatching {
            val servers = repository.getAllServersOnce()
            Log.d(TAG, "worker start count=${servers.size}")

            servers.forEach { server ->
                val pingResult = withTimeoutOrNull(CHECK_TIMEOUT_MS) {
                    checkServer(server)
                } ?: PingResult(
                    isUp = false,
                    latencyMs = CHECK_TIMEOUT_MS.toInt(),
                    checkedAt = System.currentTimeMillis(),
                    source = "worker-timeout"
                )

                Log.d(
                    TAG,
                    "worker result ${server.hostAddress} up=${pingResult.isUp} latency=${pingResult.latencyMs} source=${pingResult.source}"
                )

                val shouldNotifyDown = shouldNotifyDown(server, pingResult)

                repository.updateServerStatus(
                    id = server.id,
                    isUp = pingResult.isUp,
                    latencyMs = pingResult.latencyMs,
                    checkedAt = pingResult.checkedAt
                )

                if (pingResult.isUp) {
                    clearDownAlert(server.id)
                } else if (shouldNotifyDown) {
                    val sent = showDownNotification(server)
                    if (sent) {
                        markDownAlert(server.id)
                    }
                } else {
                    Log.d(TAG, "notify skipped already-alerted serverId=${server.id}")
                }
            }
            Result.success()
        }.getOrElse {
            Log.d(TAG, "worker failed error=${it.javaClass.simpleName}:${it.message}")
            Result.retry()
        }
    }

    private suspend fun checkServer(server: ServerEntity): PingResult {
        return autoPinger.ping(server.hostAddress)
    }

    private fun shouldNotifyDown(server: ServerEntity, pingResult: PingResult): Boolean {
        if (pingResult.isUp) {
            return false
        }

        return !alertPreferences.getBoolean(downAlertKey(server.id), false)
    }

    @SuppressLint("MissingPermission")
    private fun showDownNotification(server: ServerEntity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.d(TAG, "notify skipped missing-permission serverId=${server.id}")
                return false
            }
        }

        createNotificationChannel()
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (!notificationManager.areNotificationsEnabled()) {
            Log.d(TAG, "notify skipped disabled serverId=${server.id}")
            return false
        }

        if (!isNotificationChannelEnabled()) {
            Log.d(TAG, "notify skipped channel-disabled serverId=${server.id}")
            return false
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            server.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.server_down_title))
            .setContentText(
                applicationContext.getString(R.string.server_down_message, server.serverName)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(applicationContext.getString(R.string.server_down_message, server.serverName))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return runCatching {
            notificationManager.notify(server.id, notification)
            Log.d(TAG, "notify sent serverId=${server.id}")
            true
        }.getOrElse {
            Log.d(TAG, "notify failed serverId=${server.id} error=${it.javaClass.simpleName}:${it.message}")
            false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun isNotificationChannelEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun markDownAlert(serverId: Int) {
        alertPreferences.edit()
            .putBoolean(downAlertKey(serverId), true)
            .apply()
    }

    private fun clearDownAlert(serverId: Int) {
        alertPreferences.edit()
            .remove(downAlertKey(serverId))
            .apply()
    }

    private fun downAlertKey(serverId: Int): String {
        return "down_alert_sent_$serverId"
    }

    private val alertPreferences by lazy {
        applicationContext.getSharedPreferences(DOWN_ALERT_PREFERENCES, Context.MODE_PRIVATE)
    }

    companion object {
        const val WORK_NAME = "pingmon_periodic_ping"
        private const val CHECK_TIMEOUT_MS = 10_000L
        private const val CHANNEL_ID = "pingmon_alerts"
        private const val DOWN_ALERT_PREFERENCES = "pingmon_down_alerts"
        private const val TAG = "PingMonDebug"
    }
}
