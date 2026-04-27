package cloud.meis

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import cloud.meis.model.DummyUsers

class MainActivity : ComponentActivity() {

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
                    val btnTogglePassword = view.findViewById<ImageButton>(R.id.btnTogglePassword)

                    var isPasswordVisible = false
                    btnTogglePassword.setOnClickListener {
                        isPasswordVisible = !isPasswordVisible
                        val cursorPos = etPassword.selectionEnd
                        if (isPasswordVisible) {
                            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            btnTogglePassword.setImageResource(R.drawable.ic_eye_on)
                        } else {
                            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
                        }
                        etPassword.setSelection(cursorPos.coerceAtMost(etPassword.text.length))
                    }

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
                        val inputUsername = etUsername.text.toString().trim()
                        val inputPass = etPassword.text.toString().trim()

                        val matchedUser = DummyUsers.list.find {
                            it.username.equals(inputUsername, ignoreCase = true) && it.pass == inputPass
                        }

                        if (matchedUser != null) {
                            Toast.makeText(context, "Selamat Datang, ${matchedUser.name}!", Toast.LENGTH_LONG).show()

                            val intent = Intent(context, HomeActivity::class.java).apply {
                                putExtra("USER_NAME", matchedUser.name)
                            }
                            context.startActivity(intent)
                            finish()
                        } else {
                            it.animate().translationX(15f).setDuration(40).withEndAction {
                                it.animate().translationX(-15f).setDuration(40).withEndAction {
                                    it.animate().translationX(0f).setDuration(40).start()
                                }.start()
                            }.start()

                            Toast.makeText(context, getString(R.string.auth_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                    view
                }
            )
        }
    }
}