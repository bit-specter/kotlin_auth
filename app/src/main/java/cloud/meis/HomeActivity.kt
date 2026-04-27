package cloud.meis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cloud.meis.data.network.NetworkModule
import cloud.meis.data.repository.MovieRepository
import cloud.meis.ui.movies.MovieGridAdapter
import cloud.meis.ui.movies.MovieViewModel
import cloud.meis.ui.movies.MovieViewModelFactory

class HomeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "HomeActivity"
    }

    private val viewModel: MovieViewModel by viewModels {
        MovieViewModelFactory(MovieRepository(NetworkModule.tmdbApi))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val userName = intent.getStringExtra("USER_NAME") ?: "Guest"

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val view = LayoutInflater.from(context).inflate(R.layout.activity_home, null)

                    val tvAppBarTitle = view.findViewById<TextView>(R.id.tvAppBarTitle)
                    val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
                    val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
                    val appBar = view.findViewById<View>(R.id.appBar)
                    val rvMovies = view.findViewById<RecyclerView>(R.id.rvMovies)
                    val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
                    val tvError = view.findViewById<TextView>(R.id.tvError)
                    val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

                    ViewCompat.setOnApplyWindowInsetsListener(appBar) { appBarView, insets ->
                        val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                        appBarView.setPadding(
                            appBarView.paddingLeft,
                            statusBarTop,
                            appBarView.paddingRight,
                            appBarView.paddingBottom
                        )
                        insets
                    }

                    val movieAdapter = MovieGridAdapter { movie ->
                        Log.d(TAG, "Movie clicked: id=${movie.id}, title=${movie.title}, rating=${movie.voteAverage}")
                        val detailIntent = Intent(context, MovieDetailActivity::class.java).apply {
                            putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.id)
                            putExtra(MovieDetailActivity.EXTRA_TITLE, movie.title)
                            putExtra(MovieDetailActivity.EXTRA_OVERVIEW, movie.overview)
                            putExtra(MovieDetailActivity.EXTRA_RELEASE_DATE, movie.releaseDate)
                            putExtra(MovieDetailActivity.EXTRA_LANGUAGE, movie.originalLanguage)
                            putExtra(MovieDetailActivity.EXTRA_POPULARITY, movie.popularity)
                            putExtra(MovieDetailActivity.EXTRA_RATING, movie.voteAverage)
                            putExtra(MovieDetailActivity.EXTRA_BACKDROP_PATH, movie.backdropPath)
                            putExtra(MovieDetailActivity.EXTRA_POSTER_PATH, movie.posterPath)
                        }
                        startActivity(detailIntent)
                    }

                    rvMovies.apply {
                        layoutManager = GridLayoutManager(context, 2)
                        adapter = movieAdapter
                    }

                    tvAppBarTitle.text = getString(R.string.home_welcome)
                    tvSubtitle.text = getString(R.string.movies_welcome_user, userName)
                    tvSubtitle.isSelected = true

                    view.translationY = 50f
                    view.alpha = 0f
                    view.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(600)
                        .start()

                    viewModel.movies.observe(this@HomeActivity) { movies ->
                        if (viewModel.isLoading.value != true) {
                            Log.d(TAG, "Movies received: count=${movies.size}")
                        }
                        movieAdapter.submitList(movies)
                        val showError = movies.isEmpty() && !viewModel.error.value.isNullOrBlank()
                        tvError.visibility = if (showError) View.VISIBLE else View.GONE
                        val showEmpty = movies.isEmpty() && viewModel.error.value.isNullOrBlank() && viewModel.isLoading.value != true
                        tvEmpty.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    }

                    viewModel.isLoading.observe(this@HomeActivity) { loading ->
                        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                        if (loading) {
                            tvEmpty.visibility = View.GONE
                        }
                    }

                    viewModel.error.observe(this@HomeActivity) { errorMessage ->
                        if (!errorMessage.isNullOrBlank()) {
                            Log.d(TAG, "Movies error: $errorMessage")
                        }
                        tvError.text = if (errorMessage.isNullOrBlank()) {
                            getString(R.string.movies_retry)
                        } else {
                            getString(R.string.movies_load_failed)
                        }
                        val showError = !errorMessage.isNullOrBlank() && viewModel.movies.value.isNullOrEmpty()
                        tvError.visibility = if (showError) View.VISIBLE else View.GONE
                        if (!showError && viewModel.movies.value.isNullOrEmpty() && viewModel.isLoading.value != true) {
                            tvEmpty.visibility = View.VISIBLE
                        }
                    }

                    tvError.setOnClickListener {
                        viewModel.loadPopularMovies(language = "en-US", page = 1)
                    }

                    btnLogout?.setOnClickListener {
                        val intent = Intent(context, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    if (viewModel.movies.value.isNullOrEmpty()) {
                        viewModel.loadPopularMovies(language = "en-US", page = 1)
                    }

                    view
                }
            )
        }
    }
}