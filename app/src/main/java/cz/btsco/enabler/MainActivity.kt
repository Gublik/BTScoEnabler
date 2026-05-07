package cz.btsco.enabler

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var btnToggle: Button
    private lateinit var tvStatus: TextView
    private var scoActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        btnToggle = findViewById(R.id.btnToggle)
        tvStatus = findViewById(R.id.tvStatus)

        requestPermissions()
        updateUI()

        btnToggle.setOnClickListener {
            if (scoActive) {
                stopSco()
            } else {
                startSco()
            }
        }
    }

    private fun startSco() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val scoDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_HEADSET
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && scoDevice != null) {
            audioManager.setCommunicationDevice(scoDevice)
        } else {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
        }

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        val intent = Intent(this, ScoService::class.java)
        ContextCompat.startForegroundService(this, intent)

        scoActive = true
        updateUI()
    }

    private fun stopSco() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
        }

        audioManager.mode = AudioManager.MODE_NORMAL
        stopService(Intent(this, ScoService::class.java))

        scoActive = false
        updateUI()
    }

    private fun updateUI() {
        if (scoActive) {
            btnToggle.text = "🎧 MIKROFON SLUCHÁTEK AKTIVNÍ\n(klepni pro vypnutí)"
            btnToggle.setBackgroundColor(0xFF2E7D32.toInt())
            tvStatus.text = "✅ BT mikrofon je aktivní.\nSpusť Wispr Flow a diktuj!"
        } else {
            btnToggle.text = "🎤 Aktivovat mikrofon sluchátek"
            btnToggle.setBackgroundColor(0xFF1565C0.toInt())
            tvStatus.text = "⬆️ Nejprve připoj OnePlus Buds 4,\npak klepni na tlačítko."
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 1)
        }
    }
}
