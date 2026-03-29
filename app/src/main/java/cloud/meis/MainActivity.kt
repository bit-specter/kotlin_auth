package cloud.meis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// Data class to simulate our JSON user objects
data class UserProfile(
    val nim: String,
    val name: String,
    val pass: String
)

class MainActivity : ComponentActivity() {

    // Simulating a JSON response / Local Database
    private val dummyUsers = listOf(
        UserProfile("21552011052", "Rifky Abdul Hanan", "123456"),
        UserProfile("21552011053", "Jesslyn Eklesia", "654321") // The "Other One"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val view = LayoutInflater.from(context).inflate(R.layout.activity_login, null)
                    val btnGoogle = view.findViewById<LinearLayout>(R.id.btnGoogle)
                    val btnApple = view.findViewById<LinearLayout>(R.id.btnApple)

                    val etUsername = view.findViewById<EditText>(R.id.etUsername)
                    val etPassword = view.findViewById<EditText>(R.id.etPassword)
                    val btnLogin = view.findViewById<Button>(R.id.btnLogin)

                    // ENTRANCE ANIMATION
                    view.translationY = 80f
                    view.alpha = 0f
                    view.animate().translationY(0f).alpha(1f).setDuration(800).start()

                    btnGoogle.setOnClickListener {
                        Toast.makeText(context, "Google Login (Coming Soon)", Toast.LENGTH_SHORT).show()
                    }

                    btnApple.setOnClickListener {
                        Toast.makeText(context, "Apple Login (Coming Soon)", Toast.LENGTH_SHORT).show()
                    }
                    btnLogin.setOnClickListener {
                        val inputNim = etUsername.text.toString().trim()
                        val inputPass = etPassword.text.toString().trim()

                        // Check if any user in our "JSON" list matches the input
                        val matchedUser = dummyUsers.find { it.nim == inputNim && it.pass == inputPass }

                        if (matchedUser != null) {
                            Toast.makeText(context, "Selamat Datang, ${matchedUser.name}!", Toast.LENGTH_LONG).show()

                            val intent = Intent(context, HomeActivity::class.java).apply {
                                // Optional: Pass the name to the Home Screen
                                putExtra("USER_NAME", matchedUser.name)
                            }
                            context.startActivity(intent)
                            finish()
                        } else {
                            // Error Animation (Shake)
                            it.animate().translationX(15f).setDuration(40).withEndAction {
                                it.animate().translationX(-15f).setDuration(40).withEndAction {
                                    it.animate().translationX(0f).setDuration(40).start()
                                }.start()
                            }.start()

                            Toast.makeText(context, "NIM atau Password Salah", Toast.LENGTH_SHORT).show()
                        }
                    }
                    view
                }
            )
        }
    }
}