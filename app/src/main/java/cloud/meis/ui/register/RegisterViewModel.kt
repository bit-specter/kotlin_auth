package cloud.meis.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.meis.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val repository: UserRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<RegisterEvent>()
    val events: SharedFlow<RegisterEvent> = _events.asSharedFlow()

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        val cleanConfirm = confirmPassword.trim()

        when {
            cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank() || cleanConfirm.isBlank() -> {
                emit(RegisterEvent.Error("Semua field wajib diisi"))
                return
            }
            cleanPassword != cleanConfirm -> {
                emit(RegisterEvent.Error("Password dan konfirmasi password tidak sama"))
                return
            }
        }

        viewModelScope.launch {
            val existing = repository.getUserByEmail(cleanEmail)
            if (existing != null) {
                _events.emit(RegisterEvent.Error("Email sudah terdaftar"))
                return@launch
            }

            try {
                val userId = repository.registerUser(cleanName, cleanEmail, cleanPassword).toInt()
                _events.emit(RegisterEvent.Success(userId, cleanName))
            } catch (_: Throwable) {
                _events.emit(RegisterEvent.Error("Pendaftaran gagal"))
            }
        }
    }

    private fun emit(event: RegisterEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}