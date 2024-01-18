package sanity.wearsportstopwatch

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private const val CHANNEL_NAME = "CHANNEL_TIMER"
private lateinit var notificationManager: NotificationManager


class MyService : Service() {
    private val ONGOING_NOTIFICATION_ID = 128

    var isRunning = false
    var mMediaPlayer = MediaPlayer()
    var startTime = System.currentTimeMillis() / 1000
    var lapTime = 10

    // Future for the scheduled task
    private var future: ScheduledFuture<*>? = null

    // Binder given to clients
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MyService = this@MyService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        lapTime = 60 * intent.getIntExtra("MINUTES", 5)
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

        val notification: Notification = Notification.Builder(this, CHANNEL_NAME)
            .setContentTitle("Timer")
            .setContentText("Preparing...")
            .setSmallIcon(androidx.legacy.v4.R.drawable.notification_bg)
            .setContentIntent(pendingIntent)
            .setTicker("Ticker")
            .setOngoing(true)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    fun stopThread() {
        future?.cancel(true)
        isRunning = false
        stopForeground(true)
    }

    fun startThread() {
        startForeground()
        isRunning = false
        startTime = System.currentTimeMillis() / 1000
        isRunning = true

        val mNotifyMgr = NotificationManagerCompat.from(applicationContext)


        val scheduler = Executors.newSingleThreadScheduledExecutor()
        future = scheduler.scheduleAtFixedRate({
            val time = lapTime + (startTime - System.currentTimeMillis() / 1000)
            val timeString = (time / 60).toString() + " : " + (time % 60).toString()
            Log.d("time", timeString)

            mNotifyMgr.notify(ONGOING_NOTIFICATION_ID, generateNotification(timeString))

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

            }
        }, 0, 1, TimeUnit.SECONDS)
    }


    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest Walking Points while a
     * workout is active.
     */
    private fun generateNotification(mainText: String): Notification {
        Log.d("MyService", "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data (note, the main notification text comes from the parameter above).
        val titleText = getString(R.string.app_name)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 4. Build and issue the notification.
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, CHANNEL_NAME)

        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setSmallIcon(R.drawable.notification)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Makes Notification an Ongoing Notification (a Notification with a background task).
            .setOngoing(true)
            // For an Ongoing Activity, used to decide priority on the watch face.
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Create an Ongoing Activity.
        val ongoingActivityStatus = Status.Builder()
            // Sets the text used across various surfaces.
            .addTemplate(mainText)
            .build()

        val ongoingActivity =
            OngoingActivity.Builder(
                applicationContext,
                ONGOING_NOTIFICATION_ID,
                notificationBuilder
            )
                // Sets icon that will appear on the watch face in active mode. If it isn't set,
                // the watch face will use the static icon in active mode.
                .setAnimatedIcon(R.drawable.notification)
                // Sets the icon that will appear on the watch face in ambient mode.
                // Falls back to Notification's smallIcon if not set. If neither is set,
                // an Exception is thrown.
                .setStaticIcon(R.drawable.notification)
                // Sets the tap/touch event, so users can re-enter your app from the
                // other surfaces.
                // Falls back to Notification's contentIntent if not set. If neither is set,
                // an Exception is thrown.
                .setTouchIntent(activityPendingIntent)
                // In our case, sets the text used for the Ongoing Activity (more options are
                // available for timers and stop watches).
                .setStatus(ongoingActivityStatus)
                .build()

        // Applies any Ongoing Activity updates to the notification builder.
        // This method should always be called right before you build your notification,
        // since an Ongoing Activity doesn't hold references to the context.
        ongoingActivity.apply(applicationContext)


        return notificationBuilder.build()
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
        val mChannel = NotificationChannel(CHANNEL_NAME, name, importance)
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