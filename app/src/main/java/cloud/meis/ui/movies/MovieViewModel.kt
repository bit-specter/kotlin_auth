package cloud.meis.ui.movies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.meis.data.model.Movie
import cloud.meis.data.repository.MovieRepository
import kotlinx.coroutines.launch

class MovieViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _movies = MutableLiveData<List<Movie>>(emptyList())
    val movies: LiveData<List<Movie>> = _movies

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadPopularMovies(
        language: String = "en-US",
        page: Int = 1,
        region: String? = null
    ) {
        if (_isLoading.value == true) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getPopularMovies(language = language, page = page, region = region)
                .onSuccess { response ->
                    _movies.value = response.results
                }
                .onFailure { throwable ->
                    _error.value = throwable.message ?: "Unknown error"
                }

            _isLoading.value = false
        }
    }
}
