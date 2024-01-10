package sanity.wearsportstopwatch

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import sanity.wearsportstopwatch.databinding.ActivityMainBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var scrollView: NestedScrollView
    val time = System.currentTimeMillis()
    var startTime = System.currentTimeMillis() / 1000
    val lapTime = 3
    var isRunning = false
    var isRunningCurrent = false
    var mMediaPlayer = MediaPlayer()
    lateinit var timeTv: TextView
    lateinit var startBt: Button

    private lateinit var mService: MyService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyService.LocalBinder
            mService = binder.getService()
            mBound = true
            isRunning = mService.isRunning
            if (isRunning) {
                startBt.text = "STOP"
                startBt.setBackgroundColor(Color.parseColor("#ff0000"))
            }
            Log.e("Test", "BIND")
            startUpdateThread(timeTv)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            Log.e("Test", "UNBIND")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        isRunningCurrent = true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scrollView = binding.scrollView
        timeTv = findViewById(R.id.time)
        val currentTv = findViewById<TextView>(R.id.currentTimeTextView)
        startBt = findViewById(R.id.buttonStart)

        val minutes = findViewById<TextView>(R.id.minutes)

        val plus = findViewById<Button>(R.id.plus)
        plus.setOnClickListener(View.OnClickListener {
            minutes.text = (Integer.parseInt(minutes.text.toString()) + 1).toString()
        })

        val minus = findViewById<Button>(R.id.minus)
        minus.setOnClickListener(View.OnClickListener {
            minutes.text = (Integer.parseInt(minutes.text.toString()) - 1).toString()
        })


        timeTv.text = "STOP"

        // Post a task to scroll to the startBt in the center after the layout is complete
        scrollView.post {
            val scrollViewHeight = scrollView.height
            val startBtTop = startBt.top
            val startBtHeight = startBt.height

            val scrollTo = startBtTop + startBtHeight / 2 - scrollViewHeight / 2
            scrollView.smoothScrollTo(0, scrollTo)
        }

        startBt.setOnClickListener(View.OnClickListener {
            if (!isRunning) {
//                startTime = System.currentTimeMillis() / 1000
//                isRunning = true
//                startThread(timeTv, currentTv)



                val intent = Intent(this, MyService::class.java)
                intent.putExtra("MINUTES", Integer.parseInt(minutes.text.toString()))
                applicationContext.startForegroundService(intent)
                isRunning = true

                startUpdateThread(timeTv)

                startBt.text = "STOP"
                startBt.setBackgroundColor(Color.parseColor("#ff0000"))
                // Scroll to the top of the ScrollView
                scrollView.smoothScrollTo(0, 0)
            } else {
                isRunning = false
                timeTv.text = "STOP"
                mService.stopThread()
                startBt.text = "START"
                startBt.setBackgroundColor(Color.parseColor("#4CAF50"))
            }


        })

        Thread {
            while (isRunningCurrent) {
                currentTv.post {
                    val current = LocalDateTime.now()

                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val formatted = current.format(formatter)
                    currentTv.text = formatted
                }
                Thread.sleep(250)
            }
        }.start()
    }

    private fun startUpdateThread(timeTv: TextView) {
        Thread {
            while (isRunning) {
                timeTv.post {
                    val formatted = mService.getFormattedTime()
                    timeTv.text = formatted
                }
                Thread.sleep(250)
            }
        }.start()
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        Intent(this, MyService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunningCurrent = false
    }

}