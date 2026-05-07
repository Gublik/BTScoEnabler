package cz.btsco.enabler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class ScoService : Service() {

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val keepAliveRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                refreshSco()
                handler.postDelayed(this, 7000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, "sco_channel")
            .setContentTitle("🎧 BT Mic Enabler aktivní")
            .setContentText("Mikrofon sluchátek dostupný pro Wispr Flow")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        isRunning = true
        handler.post(keepAliveRunnable)

        return START_STICKY
    }

    private fun refreshSco() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val scoDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }
                if (scoDevice != null) {
                    audioManager.setCommunicationDevice(scoDevice)
                }
            } else {
                @Suppress("DEPRECATION")
                if (!audioManager.isBluetoothScoOn) {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
            }
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(keepAliveRunnable)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "sco_channel", "BT Mikrofon",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Udržuje Bluetooth SCO aktivní" }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
