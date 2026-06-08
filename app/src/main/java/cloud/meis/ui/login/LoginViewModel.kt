package cloud.meis.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.meis.data.model.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val dummyUsers = listOf(
        UserProfile("rifky@pingmon.test", "Rifky Abdul Hanan", "123456"),
        UserProfile("jesslyn@pingmon.test", "Jesslyn Eklesia", "654321")
    )

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun login(email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        val normalizedPassword = password.trim()
        val matchedUser = dummyUsers.find {
            it.email == normalizedEmail && it.password == normalizedPassword
        }

        viewModelScope.launch {
            if (matchedUser != null) {
                _events.emit(LoginEvent.Success(matchedUser.name))
            } else {
                _events.emit(LoginEvent.Error("Email atau password salah"))
            }
        }
    }
}
