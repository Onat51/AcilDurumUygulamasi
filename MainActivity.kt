import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Kullanıcı daha önce isim girdiyse doğrudan ana ekrana yönlendir
        if (auth.currentUser != null) {
            startActivity(Intent(this, EmergencyActivity::class.java))
            finish()
            return
        }

        showNameInputDialog()
    }

    private fun showNameInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_name_input, null)
        val editTextName = dialogView.findViewById<EditText>(R.id.editTextName)

        AlertDialog.Builder(this)
            .setTitle("İsminizi Girin")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val name = editTextName.text.toString().trim()
                if (name.isNotEmpty()) {
                    createAnonymousUser(name)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun createAnonymousUser(name: String) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userRef = database.reference.child("users").child(userId)

                    // Kullanıcı bilgilerini Firebase'e kaydet
                    userRef.child("name").setValue(name)
                    userRef.child("emergency").setValue(0)
                    userRef.child("location").child("lat").setValue(0.0)
                    userRef.child("location").child("lng").setValue(0.0)

                    startActivity(Intent(this, EmergencyActivity::class.java))
                    finish()
                }
            }
    }
}