package cloud.meis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cloud.meis.data.local.database.AppDatabase
import cloud.meis.data.local.preference.SessionManager
import cloud.meis.data.repository.UserRepository
import cloud.meis.ui.register.RegisterEvent
import cloud.meis.ui.register.RegisterViewModel
import kotlinx.coroutines.launch

class RegisterActivity : ComponentActivity() {
    private lateinit var viewModel: RegisterViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn() && sessionManager.getUserId() > 0) {
            openMain(sessionManager.getUserId(), sessionManager.getUserName(), showWelcome = false)
            return
        }

        setContentView(R.layout.activity_register)
        applySystemBarInsets()

        val repository = UserRepository(AppDatabase.getInstance(applicationContext).userDao())
        viewModel = ViewModelProvider(this, RegisterViewModelFactory(repository))[RegisterViewModel::class.java]

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            viewModel.register(
                fullName = etFullName.text.toString(),
                email = etEmail.text.toString(),
                password = etPassword.text.toString(),
                confirmPassword = etConfirmPassword.text.toString()
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RegisterEvent.Success -> {
                            sessionManager.saveLogin(event.userId, event.userName)
                            Toast.makeText(this@RegisterActivity, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                            openMain(event.userId, event.userName)
                        }
                        is RegisterEvent.Error -> Toast.makeText(this@RegisterActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.rootRegister)
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

    private fun openMain(userId: Int, userName: String, showWelcome: Boolean = true) {
        if (showWelcome) {
            Toast.makeText(this, "Selamat Datang, $userName!", Toast.LENGTH_LONG).show()
        }
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("USER_NAME", userName)
        })
        finish()
    }
}