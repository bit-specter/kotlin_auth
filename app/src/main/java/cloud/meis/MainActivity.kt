package cloud.meis

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cloud.meis.data.local.database.AppDatabase
import cloud.meis.data.local.preference.SessionManager
import cloud.meis.data.repository.ServerRepository
import cloud.meis.ui.main.MainViewModel
import cloud.meis.ui.main.ServerAdapter
import cloud.meis.worker.PingWorker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: ServerAdapter
    private lateinit var sessionManager: SessionManager
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                getString(R.string.notification_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()
        sessionManager = SessionManager(this)

        val repository = ServerRepository(
            AppDatabase.getInstance(applicationContext).serverDao()
        )
        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(repository)
        )[MainViewModel::class.java]

        adapter = ServerAdapter { server ->
            viewModel.deleteServer(server)
        }

        val recyclerServers = findViewById<RecyclerView>(R.id.recyclerServers)
        val emptyServers = findViewById<TextView>(R.id.tvEmptyServers)
        val debugLog = findViewById<TextView>(R.id.tvDebugLog)
        val userName = intent.getStringExtra("USER_NAME") ?: sessionManager.getUserName()
        findViewById<TextView>(R.id.tvLoggedInUser).text = getString(
            R.string.logged_in_user,
            userName
        )
        recyclerServers.layoutManager = LinearLayoutManager(this)
        recyclerServers.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddServer).setOnClickListener {
            showAddServerDialog()
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clear()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnCheckNow).setOnClickListener {
            viewModel.checkAllServers()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        adapter.submitList(state.servers)
                        emptyServers.isVisible = !state.isLoading && state.servers.isEmpty()
                        debugLog.text = state.debugLogs.joinToString(separator = "\n")
                        updateCheckButton(state.isChecking)
                    }
                }
                launch {
                    viewModel.events.collect { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        requestNotificationPermission()
        schedulePingWorker()
        viewModel.checkAllServers()
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.rootMain)
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

    private fun updateCheckButton(isChecking: Boolean) {
        val button = findViewById<Button>(R.id.btnCheckNow)
        button.isEnabled = !isChecking
        button.text = if (isChecking) {
            getString(R.string.checking)
        } else {
            getString(R.string.check_now)
        }
    }

    private fun showAddServerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_server, null)
        val serverNameInput = dialogView.findViewById<EditText>(R.id.etServerName)
        val hostAddressInput = dialogView.findViewById<EditText>(R.id.etHostAddress)
        val cancelButton = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val saveButton = dialogView.findViewById<Button>(R.id.btnDialogSave)
        applyLowercaseInput(hostAddressInput)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            saveButton.setOnClickListener {
                val added = viewModel.addServer(
                    serverName = serverNameInput.text.toString(),
                    hostAddress = hostAddressInput.text.toString()
                )

                if (added) {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun applyLowercaseInput(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var editing = false

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (editing || s == null) {
                    return
                }

                val current = s.toString()
                val lower = current.lowercase()
                if (current == lower) {
                    return
                }

                editing = true
                val selection = editText.selectionStart.coerceIn(0, lower.length)
                editText.setText(lower)
                editText.setSelection(selection)
                editing = false
            }
        })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun schedulePingWorker() {
        val request = PeriodicWorkRequestBuilder<PingWorker>(
            PingWorker.REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(pingConstraints())
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            PingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.d(
            TAG,
            "periodic worker scheduled interval=${PingWorker.REPEAT_INTERVAL_MINUTES} minutes"
        )
    }

    private fun pingConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    companion object {
        private const val TAG = "PingMonDebug"
    }
}
