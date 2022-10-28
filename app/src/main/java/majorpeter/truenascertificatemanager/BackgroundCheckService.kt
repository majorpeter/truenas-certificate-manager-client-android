package majorpeter.truenascertificatemanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager

class BackgroundCheckService: JobService() {
    companion object {
        private const val TAG = "BackgroundCheckService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_RENEW_REMINDER = "renew_reminder"
    }

    private fun doBackgroundWork(params: JobParameters?) {
        Thread(Runnable {
            val remainingDays = CertificateHelper(this).getRemainingDays()
            val requiredCertLifetime = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("required_cert_lifetime", "0")!!.toInt()

            if (remainingDays.isSuccess) {
                if (remainingDays.getOrNull()!! <= requiredCertLifetime) {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent =
                        PendingIntent.getActivity(
                            this,
                            0,
                            intent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                        )

                    val builder = NotificationCompat.Builder(this, CHANNEL_RENEW_REMINDER)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(getString(R.string.cert_about_to_expire))
                        .setContentText(
                            getString(R.string.cert_expires_in_n_days).format(
                                remainingDays.getOrNull()
                            )
                        )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                    with(NotificationManagerCompat.from(this)) {
                        createNotificationChannel(
                            NotificationChannel(
                                CHANNEL_RENEW_REMINDER,
                                getString(R.string.certificate_renew_reminder),
                                NotificationManager.IMPORTANCE_DEFAULT
                            )
                        )
                        notify(NOTIFICATION_ID, builder.build())
                    }
                }
            }
            jobFinished(params, true)
        }).start()
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "started")
        doBackgroundWork(params)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "stopped")
        return true
    }
}
