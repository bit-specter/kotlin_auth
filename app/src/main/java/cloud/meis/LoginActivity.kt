package cloud.meis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cloud.meis.data.local.preference.SessionManager
import cloud.meis.ui.login.LoginEvent
import cloud.meis.ui.login.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            openMain(sessionManager.getUserName(), showWelcome = false)
            return
        }

        setContentView(R.layout.activity_login)
        applySystemBarInsets()

        val btnGoogle = findViewById<LinearLayout>(R.id.btnGoogle)
        val btnApple = findViewById<LinearLayout>(R.id.btnApple)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        window.decorView.translationY = 80f
        window.decorView.alpha = 0f
        window.decorView.animate().translationY(0f).alpha(1f).setDuration(800).start()

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google Login (Coming Soon)", Toast.LENGTH_SHORT).show()
        }

        btnApple.setOnClickListener {
            Toast.makeText(this, "Apple Login (Coming Soon)", Toast.LENGTH_SHORT).show()
        }

        btnLogin.setOnClickListener {
            viewModel.login(
                email = etEmail.text.toString(),
                password = etPassword.text.toString()
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is LoginEvent.Success -> {
                            sessionManager.saveLogin(event.userName)
                            openMain(event.userName)
                        }
                        is LoginEvent.Error -> showLoginError(btnLogin, event.message)
                    }
                }
            }
        }
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.rootLogin)
        val baseLeft = root.paddingLeft
        val baseTop = root.paddingTop
        val baseRight = root.paddingRight
        val baseBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                baseLeft + systemBars.left,
                baseTop + systemBars.top,
                baseRight + systemBars.right,
                baseBottom + systemBars.bottom
            )
            insets
        }
    }

    private fun openMain(userName: String, showWelcome: Boolean = true) {
        if (showWelcome) {
            Toast.makeText(this, "Selamat Datang, $userName!", Toast.LENGTH_LONG).show()
        }
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("USER_NAME", userName)
        })
        finish()
    }

    private fun showLoginError(view: View, message: String) {
        view.animate().translationX(15f).setDuration(40).withEndAction {
            view.animate().translationX(-15f).setDuration(40).withEndAction {
                view.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
