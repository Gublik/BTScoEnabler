package cz.btsco.enabler

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            when (state) {
                AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                    scoActive = true
                    val service = Intent(context, ScoService::class.java)
                    ContextCompat.startForegroundService(context, service)
                    setUI("✅ SCO PŘIPOJENO!\nSpusť Wispr Flow a mluv.", true)
                }
                AudioManager.SCO_AUDIO_STATE_CONNECTING -> {
                    setUI("⏳ Připojuji BT mikrofon...", false)
                }
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                    if (scoActive) {
                        scoActive = false
                        stopService(Intent(context, ScoService::class.java))
                        setUI("⚠️ SCO odpojeno systémem.\nZkus znovu.", false)
                    }
                }
                AudioManager.SCO_AUDIO_STATE_ERROR -> {
                    setUI("❌ SCO chyba - sluchátka nepodporují HFP?", false)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        btnToggle = findViewById(R.id.btnToggle)
        tvStatus = findViewById(R.id.tvStatus)

        registerReceiver(scoReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))

        requestPermissions()
        setUI("Připoj OnePlus Buds 4,\npak klepni na tlačítko.", false)

        btnToggle.setOnClickListener {
            if (scoActive) stopSco() else startSco()
        }
    }

    private fun startSco() {
        setUI("⏳ Spouštím...", false)
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val scoDevice = inputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            if (scoDevice != null) {
                val ok = audioManager.setCommunicationDevice(scoDevice)
                if (ok) {
                    scoActive = true
                    ContextCompat.startForegroundService(
                        this, Intent(this, ScoService::class.java))
                    setUI("✅ BT mikrofon aktivní!\nSpusť Wispr Flow.", true)
                } else {
                    setUI("❌ setCommunicationDevice selhalo.\nZkouším starší metodu...", false)
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                }
            } else {
                setUI("❌ SCO vstup nenalezen!\nJsou sluchátka připojena?", false)
                @Suppress("DEPRECATION")
                audioManager.startBluetoothSco()
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
        }
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
        setUI("Vypnuto. Klepni pro aktivaci.", false)
    }

    private fun setUI(status: String, active: Boolean) {
        runOnUiThread {
            tvStatus.text = status
            if (active) {
                btnToggle.text = "🎧 AKTIVNÍ\n(klepni pro vypnutí)"
                btnToggle.setBackgroundColor(0xFF2E7D32.toInt())
            } else {
                btnToggle.text = "🎤 Aktivovat BT mikrofon"
                btnToggle.setBackgroundColor(0xFF1565C0.toInt())
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty())
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(scoReceiver) } catch (e: Exception) {}
    }
}
