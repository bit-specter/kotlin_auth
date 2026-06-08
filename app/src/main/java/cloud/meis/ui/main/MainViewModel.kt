package cloud.meis.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cloud.meis.data.local.entity.ServerEntity
import cloud.meis.data.model.PingResult
import cloud.meis.data.model.ServerProtocol
import cloud.meis.data.repository.ServerRepository
import cloud.meis.network.AutoPinger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainViewModel(
    private val repository: ServerRepository,
    private val autoPinger: AutoPinger = AutoPinger()
) : ViewModel() {
    private val isChecking = MutableStateFlow(false)
    private val debugLogs = MutableStateFlow<List<String>>(emptyList())
    private var checkJob: Job? = null

    val uiState = combine(
        repository.getAllServers(),
        isChecking,
        debugLogs
    ) { servers, checking, logs ->
            MainUiState(
                servers = servers,
                isLoading = false,
                isChecking = checking,
                debugLogs = logs
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = MainUiState()
        )

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun addServer(
        serverName: String,
        hostAddress: String
    ): Boolean {
        val cleanName = serverName.trim()
        val cleanHost = hostAddress.trim().lowercase()

        if (cleanName.isBlank() || cleanHost.isBlank()) {
            emitEvent("Nama server dan host wajib diisi")
            return false
        }

        viewModelScope.launch {
            repository.addServer(
                ServerEntity(
                    serverName = cleanName,
                    hostAddress = cleanHost,
                    protocol = ServerProtocol.AUTO
                )
            )
            restartCheckAllServers()
        }

        return true
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.deleteServer(server)
            if (repository.getAllServersOnce().isEmpty()) {
                stopActiveCheck()
            }
        }
    }

    fun checkAllServers() {
        if (isChecking.value) {
            return
        }

        checkJob = viewModelScope.launch {
            val servers = repository.getAllServersOnce()
            if (servers.isEmpty()) {
                appendDebug("no targets")
                emitEvent("Tambahkan server dulu")
                return@launch
            }

            isChecking.value = true
            appendDebug("check start count=${servers.size}")
            try {
                servers.map { server ->
                    async {
                        if (repository.getServerById(server.id) == null) {
                            return@async
                        }

                        appendDebug("start ${server.hostAddress}")
                        val result = withTimeoutOrNull(CHECK_TIMEOUT_MS) {
                            checkServer(server)
                        } ?: PingResult(
                            isUp = false,
                            latencyMs = CHECK_TIMEOUT_MS.toInt(),
                            checkedAt = System.currentTimeMillis(),
                            source = "target-timeout"
                        )

                        if (repository.getServerById(server.id) == null) {
                            return@async
                        }

                        repository.updateServerStatus(
                            id = server.id,
                            isUp = result.isUp,
                            latencyMs = result.latencyMs,
                            checkedAt = result.checkedAt
                        )
                        appendDebug(
                            "done ${server.hostAddress} ${if (result.isUp) "UP" else "DOWN"} ${result.latencyMs}ms ${result.source}"
                        )
                    }
                }.awaitAll()

                if (repository.getAllServersOnce().isNotEmpty()) {
                    appendDebug("check complete")
                    emitEvent("Check selesai")
                }
            } catch (_: CancellationException) {
                appendDebug("check cancelled")
            } catch (_: Throwable) {
                appendDebug("check failed")
                emitEvent("Check gagal dijalankan")
            } finally {
                isChecking.value = false
                checkJob = null
            }
        }
    }

    private fun restartCheckAllServers() {
        stopActiveCheck()
        checkAllServers()
    }

    private fun stopActiveCheck() {
        checkJob?.cancel()
        checkJob = null
        isChecking.value = false
    }

    private suspend fun checkServer(server: ServerEntity): PingResult {
        return autoPinger.ping(server.hostAddress)
    }

    private fun emitEvent(message: String) {
        viewModelScope.launch {
            _events.emit(message)
        }
    }

    private fun appendDebug(message: String) {
        Log.d(TAG, message)
        val line = "${System.currentTimeMillis() % 100000} $message"
        debugLogs.value = (debugLogs.value + line).takeLast(MAX_DEBUG_LINES)
    }

    class Factory(
        private val repository: ServerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val CHECK_TIMEOUT_MS = 10_000L
        private const val MAX_DEBUG_LINES = 8
        private const val TAG = "PingMonDebug"
    }
}
