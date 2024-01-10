package sanity.wearsportstopwatch

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class MyService : Service() {
    private val ONGOING_NOTIFICATION_ID = 128

    var isRunning = false
    var mMediaPlayer = MediaPlayer()
    var startTime = System.currentTimeMillis() / 1000
    var lapTime = 10

    // Binder given to clients
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MyService = this@MyService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        lapTime = 60 * intent.getIntExtra("MINUTES",5)
        createChannel(this)
        startThread()
        return super.onStartCommand(intent, flags, startId)

    }

    private fun startForeground() {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification: Notification = Notification.Builder(this, "CHANNEL_TIMER")
            .setContentTitle("Timer")
            .setContentText("Preparing...")
            .setSmallIcon(androidx.legacy.v4.R.drawable.notification_bg)
            .setContentIntent(pendingIntent)
            .setTicker("Ticker")
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    fun stopThread() {
        isRunning = false
        stopForeground(true)
    }

    fun startThread() {
        startForeground()
        isRunning = false
        startTime = System.currentTimeMillis() / 1000
        isRunning = true
        Thread(Runnable {
            Thread.sleep(1100)
            while (isRunning) {
                val time = lapTime + (startTime - System.currentTimeMillis() / 1000)
                val timeString = (time / 60).toString() + " : " + (time % 60).toString()
                Log.d("time", timeString)


                val pendingIntent: PendingIntent =
                    Intent(this, MainActivity::class.java).let { notificationIntent ->
                        PendingIntent.getActivity(
                            this, 0, notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                val notification: Notification = Notification.Builder(this, "CHANNEL_TIMER")
                    .setContentTitle(timeString)
                    .setContentText(getString(R.string.open_the_app_to_stop_it))
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .setTicker("Ticker")
                    .build()

                val mNotifyMgr = NotificationManagerCompat.from(applicationContext)
                mNotifyMgr.notify(ONGOING_NOTIFICATION_ID, notification)

                if (time <= 0) {
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    val vibrationPattern = longArrayOf(0, 300, 50, 500)
                    //-1 - don't repeat
                    //-1 - don't repeat
                    val indexInPatternToRepeat = -1
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat)

                    mMediaPlayer.release()
                    mMediaPlayer = MediaPlayer.create(this, R.raw.noti)
                    mMediaPlayer.isLooping = false
                    mMediaPlayer.start()

                    startTime = System.currentTimeMillis() / 1000

                    continue
                }
                Thread.sleep(1000)
            }
        }).start()
    }

    private fun createChannel(context: Context) {
        val mNotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // The id of the channel.
        // The user-visible name of the channel.
        val name: CharSequence = "Timer"
        // The user-visible description of the channel.
        val description: String = "Timer"
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel("CHANNEL_TIMER", name, importance)
        // Configure the notification channel.
        mChannel.description = description
        mChannel.setShowBadge(true)
        mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        mNotificationManager.createNotificationChannel(mChannel)
    }

    fun getFormattedTime(): CharSequence? {
        val time = lapTime + (startTime - System.currentTimeMillis() / 1000)
        return (time / 60).toString() + " : " + (time % 60).toString();
    }
}