package cloud.meis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cloud.meis.data.repository.UserRepository
import cloud.meis.ui.login.LoginViewModel

class LoginViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}