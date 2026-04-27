package cloud.meis.data.repository

import android.util.Log
import cloud.meis.data.model.MovieCreditsResponse
import cloud.meis.data.model.MovieDetailResponse
import cloud.meis.data.model.MovieResponse
import cloud.meis.data.model.MovieVideosResponse
import cloud.meis.data.model.WatchProvidersResponse
import cloud.meis.data.network.TmdbApiService

class MovieRepository(
    private val apiService: TmdbApiService
) {
    companion object {
        private const val TAG = "MovieRepository"
    }

    suspend fun getPopularMovies(
        language: String = "en-US",
        page: Int = 1,
        region: String? = null
    ): Result<MovieResponse> {
        return try {
            val response = apiService.getPopularMovies(language = language, page = page, region = region)
            Log.d(
                TAG,
                "Popular API success. language=$language, page=$page, region=${region ?: "null"}, responsePage=${response.page}, totalPages=${response.totalPages}, totalResults=${response.totalResults}, results=${response.results.size}"
            )
            if (response.results.isEmpty()) {
                Log.d(
                    TAG,
                    "Popular API returned 0 results. Possible causes: region too strict, no localized data for language/region, page out of range, or temporary API dataset state."
                )
            }
            Log.d(TAG, "Popular API payload=${response.results}")
            Result.success(response)
        } catch (exception: Exception) {
            Log.d(TAG, "Popular API failed: ${exception.message}", exception)
            Result.failure(exception)
        }
    }

    suspend fun getMovieDetail(
        movieId: Int,
        language: String = "en-US"
    ): Result<MovieDetailResponse> {
        return try {
            val response = apiService.getMovieDetail(movieId = movieId, language = language)
            Log.d(TAG, "Movie detail success. id=${response.id}, title=${response.title}, runtime=${response.runtime}")
            Result.success(response)
        } catch (exception: Exception) {
            Log.d(TAG, "Movie detail failed: ${exception.message}", exception)
            Result.failure(exception)
        }
    }

    suspend fun getMovieCredits(
        movieId: Int,
        language: String = "en-US"
    ): Result<MovieCreditsResponse> {
        return try {
            val response = apiService.getMovieCredits(movieId = movieId, language = language)
            Log.d(TAG, "Movie credits success. id=${response.id}, castCount=${response.cast.size}")
            Result.success(response)
        } catch (exception: Exception) {
            Log.d(TAG, "Movie credits failed: ${exception.message}", exception)
            Result.failure(exception)
        }
    }

    suspend fun getMovieWatchProviders(movieId: Int): Result<WatchProvidersResponse> {
        return try {
            val response = apiService.getMovieWatchProviders(movieId = movieId)
            Log.d(TAG, "Watch providers success. id=${response.id}, regionCount=${response.results.size}")
            Result.success(response)
        } catch (exception: Exception) {
            Log.d(TAG, "Watch providers failed: ${exception.message}", exception)
            Result.failure(exception)
        }
    }

    suspend fun getMovieVideos(
        movieId: Int,
        language: String = "en-US"
    ): Result<MovieVideosResponse> {
        return try {
            val response = apiService.getMovieVideos(movieId = movieId, language = language)
            Log.d(TAG, "Videos success. id=${response.id}, count=${response.results.size}")
            Result.success(response)
        } catch (exception: Exception) {
            Log.d(TAG, "Videos failed: ${exception.message}", exception)
            Result.failure(exception)
        }
    }
}
