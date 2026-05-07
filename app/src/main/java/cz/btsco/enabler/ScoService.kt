package cz.btsco.enabler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder

class ScoService : Service() {

    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private var recordThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, "sco_channel")
            .setContentTitle("🎧 BT Mic Enabler")
            .setContentText("BT mikrofon aktivní – Wispr Flow ho může použít")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        startSilentRecording()

        return START_STICKY
    }

    private fun startSilentRecording() {
        val sampleRate = 8000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, channelConfig, audioFormat
        ).coerceAtLeast(1024)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate, channelConfig, audioFormat, bufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                isRunning = true

                recordThread = Thread {
                    val buffer = ShortArray(bufferSize)
                    while (isRunning) {
                        audioRecord?.read(buffer, 0, bufferSize)
                        Thread.sleep(10)
                    }
                }.also { it.start() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        isRunning = false
        recordThread?.interrupt()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "sco_channel", "BT Mikrofon",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Udržuje aktivní Bluetooth mikrofon" }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
