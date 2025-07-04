import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EmergencyActivity : AppCompatActivity() {

    private lateinit var emergencyButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var emergencyStatusRef: DatabaseReference
    private lateinit var locationServiceIntent: Intent
    private var vibrator: Vibrator? = null
    private var alarmSound: Uri? = null
    private var mediaPlayer: android.media.MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        emergencyButton = findViewById(R.id.emergencyButton)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val userId = auth.currentUser?.uid ?: return
        emergencyStatusRef = database.reference.child("users").child(userId).child("emergency")

        setupEmergencyButton()
        checkEmergencyStatus()
        startLocationService()
    }

    private fun setupEmergencyButton() {
        emergencyButton.setOnClickListener {
            triggerEmergency()
        }

        // Ses tuşları için dinleyici eklenebilir
    }

    private fun triggerEmergency() {
        emergencyStatusRef.setValue(1)
            .addOnSuccessListener {
                // Alarm tetiklendi bildirimi
                Handler(Looper.getMainLooper()).postDelayed({
                    emergencyStatusRef.setValue(0)
                }, 30000) // 30 saniye sonra otomatik kapat
            }
    }

    private fun checkEmergencyStatus() {
        database.reference.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var emergencyDetected = false
                var emergencyUserName = ""

                for (userSnapshot in snapshot.children) {
                    val emergencyStatus = userSnapshot.child("emergency").getValue(Int::class.java) ?: 0
                    if (emergencyStatus == 1) {
                        emergencyDetected = true
                        emergencyUserName = userSnapshot.child("name").getValue(String::class.java) ?: ""
                        break
                    }
                }

                if (emergencyDetected) {
                    activateEmergencyMode(emergencyUserName)
                } else {
                    deactivateEmergencyMode()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Hata durumu
            }
        })
    }

    private fun activateEmergencyMode(userName: String) {
        // Ekranı uyandır
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Arka planı kırmızı yap
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))

        // Alarm sesi çal
        playAlarmSound()

        // Titreşim
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(500, 500), 0))
        } else {
            vibrator?.vibrate(500)
        }

        // Alarm veren kişinin adını göster
        findViewById<TextView>(R.id.emergencyText).text = "$userName Acil Durumda!"
    }

    private fun deactivateEmergencyMode() {
        // Alarmı durdur
        mediaPlayer?.stop()
        vibrator?.cancel()

        // Normal moda dön
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        window.decorView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.background_light))
    }

    private fun playAlarmSound() {
        try {
            mediaPlayer = android.media.MediaPlayer.create(this, alarmSound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startLocationService() {
        locationServiceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, locationServiceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        stopService(locationServiceIntent)
    }
}