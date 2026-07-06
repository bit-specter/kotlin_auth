package cloud.meis.ui.login

sealed interface LoginEvent {
    data class Success(val userId: Int, val userName: String) : LoginEvent
    data class Error(val message: String) : LoginEvent
}
