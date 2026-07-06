package cloud.meis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cloud.meis.data.repository.UserRepository
import cloud.meis.ui.register.RegisterViewModel

class RegisterViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}