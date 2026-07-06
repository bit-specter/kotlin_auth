package cloud.meis.ui.register

sealed interface RegisterEvent {
    data class Success(val userId: Int, val userName: String) : RegisterEvent
    data class Error(val message: String) : RegisterEvent
}