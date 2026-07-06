package cloud.meis.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.meis.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: UserRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun login(email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        val normalizedPassword = password.trim()

        viewModelScope.launch {
            val matchedUser = repository.login(normalizedEmail, normalizedPassword)
            if (matchedUser != null) {
                _events.emit(LoginEvent.Success(matchedUser.id, matchedUser.fullName))
                return@launch
            }

            _events.emit(LoginEvent.Error("Email atau password salah"))
        }
    }
}
