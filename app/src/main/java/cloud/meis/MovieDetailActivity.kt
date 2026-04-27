package cloud.meis

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cloud.meis.data.network.NetworkModule
import cloud.meis.data.repository.MovieRepository
import cloud.meis.ui.movies.CastAdapter
import cloud.meis.ui.movies.WatchProviderAdapter
import coil.load
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MovieDetailActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MovieDetailActivity"
        const val EXTRA_MOVIE_ID = "extra_movie_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_OVERVIEW = "extra_overview"
        const val EXTRA_RELEASE_DATE = "extra_release_date"
        const val EXTRA_LANGUAGE = "extra_language"
        const val EXTRA_POPULARITY = "extra_popularity"
        const val EXTRA_RATING = "extra_rating"
        const val EXTRA_BACKDROP_PATH = "extra_backdrop_path"
        const val EXTRA_POSTER_PATH = "extra_poster_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val repository = MovieRepository(NetworkModule.tmdbApi)
        val movieId = intent.getIntExtra(EXTRA_MOVIE_ID, 0)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val overview = intent.getStringExtra(EXTRA_OVERVIEW).orEmpty()
        val releaseDate = intent.getStringExtra(EXTRA_RELEASE_DATE).orEmpty()
        val language = intent.getStringExtra(EXTRA_LANGUAGE).orEmpty().uppercase()
        val popularity = intent.getDoubleExtra(EXTRA_POPULARITY, 0.0)
        val rating = intent.getDoubleExtra(EXTRA_RATING, 0.0)
        val backdropPath = intent.getStringExtra(EXTRA_BACKDROP_PATH)
        val posterPath = intent.getStringExtra(EXTRA_POSTER_PATH)

        Log.d(
            TAG,
            "Detail data title=$title, release=$releaseDate, language=$language, popularity=$popularity, rating=$rating, backdrop=$backdropPath, poster=$posterPath"
        )

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val view = LayoutInflater.from(context).inflate(R.layout.activity_movie_detail, null)

                    val svDetail = view.findViewById<ScrollView>(R.id.svDetail)
                    val ivBackdrop = view.findViewById<ImageView>(R.id.ivBackdrop)
                    val backdropLoadingOverlay = view.findViewById<LinearLayout>(R.id.backdropLoadingOverlay)
                    val tvBackdropProgress = view.findViewById<TextView>(R.id.tvBackdropProgress)
                    val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
                    val btnPlayTrailer = view.findViewById<ImageButton>(R.id.btnPlayTrailer)
                    val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                    val tvTagline = view.findViewById<TextView>(R.id.tvTagline)
                    val tvOriginalTitle = view.findViewById<TextView>(R.id.tvOriginalTitle)
                    val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
                    val tvRatingInline = view.findViewById<TextView>(R.id.tvRatingInline)
                    val tvRuntimeInline = view.findViewById<TextView>(R.id.tvRuntimeInline)
                    val tvGenres = view.findViewById<TextView>(R.id.tvGenres)
                    val tvRelease = view.findViewById<TextView>(R.id.tvRelease)
                    val tvBudget = view.findViewById<TextView>(R.id.tvBudget)
                    val tvRevenue = view.findViewById<TextView>(R.id.tvRevenue)
                    val tvLanguage = view.findViewById<TextView>(R.id.tvLanguage)
                    val tvPopularity = view.findViewById<TextView>(R.id.tvPopularity)
                    val tvVoteCount = view.findViewById<TextView>(R.id.tvVoteCount)
                    val tvCountries = view.findViewById<TextView>(R.id.tvCountries)
                    val tvSpokenLanguages = view.findViewById<TextView>(R.id.tvSpokenLanguages)
                    val tvOriginCountry = view.findViewById<TextView>(R.id.tvOriginCountry)
                    val tvCollection = view.findViewById<TextView>(R.id.tvCollection)
                    val tvProductionCompanies = view.findViewById<TextView>(R.id.tvProductionCompanies)
                    val tvHomepage = view.findViewById<TextView>(R.id.tvHomepage)
                    val tvImdb = view.findViewById<TextView>(R.id.tvImdb)
                    val tvOverview = view.findViewById<TextView>(R.id.tvOverview)
                    val rvCast = view.findViewById<RecyclerView>(R.id.rvCast)
                    val tvCastEmpty = view.findViewById<TextView>(R.id.tvCastEmpty)
                    val tvCrew = view.findViewById<TextView>(R.id.tvCrew)
                    val tvWatchProvidersMeta = view.findViewById<TextView>(R.id.tvWatchProvidersMeta)
                    val rvWatchProviders = view.findViewById<RecyclerView>(R.id.rvWatchProviders)
                    val tvWatchProvidersEmpty = view.findViewById<TextView>(R.id.tvWatchProvidersEmpty)
                    val webViewPlayer = view.findViewById<WebView>(R.id.webViewPlayer)
                    val tvVideos = view.findViewById<TextView>(R.id.tvVideos)
                    val tvVideosTitle = view.findViewById<TextView>(R.id.tvVideosTitle)

                    webViewPlayer.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    fun openInYouTube(key: String) {
                        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$key"))
                        try {
                            context.startActivity(appIntent)
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$key"))
                            )
                        }
                    }

                    var trailerKey: String? = null
                    var isTrailerPlaying = false
                    var pulseAnimator: ObjectAnimator? = null

                    fun stopPulseAnimation() {
                        pulseAnimator?.cancel()
                        pulseAnimator = null
                        btnPlayTrailer.scaleX = 1f
                        btnPlayTrailer.scaleY = 1f
                        btnPlayTrailer.alpha = 1f
                    }

                    fun startPulseAnimation() {
                        stopPulseAnimation()
                        val sx = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.22f, 1f)
                        val sy = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.22f, 1f)
                        val al = PropertyValuesHolder.ofFloat("alpha", 1f, 0.75f, 1f)
                        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btnPlayTrailer, sx, sy, al).apply {
                            duration = 950
                            repeatCount = ObjectAnimator.INFINITE
                            start()
                        }
                    }

                    fun startTrailerPlayback() {
                        val key = trailerKey ?: return
                        stopPulseAnimation()
                        webViewPlayer.visibility = android.view.View.VISIBLE
                        webViewPlayer.isClickable = true
                        btnPlayTrailer.animate()
                            .alpha(0f).scaleX(0.7f).scaleY(0.7f)
                            .setDuration(260)
                            .withEndAction {
                                btnPlayTrailer.visibility = android.view.View.GONE
                                btnPlayTrailer.alpha = 1f
                                btnPlayTrailer.scaleX = 1f
                                btnPlayTrailer.scaleY = 1f
                            }
                            .start()
                        // iFrame API gives us onError callbacks — when embedding is blocked
                        // (error 150/152), AndroidBridge.onPlayerError opens the YouTube app instead.
                        val html = """<!DOCTYPE html>
<html><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>*{margin:0;padding:0;box-sizing:border-box}html,body{width:100%;height:100%;background:#000;overflow:hidden}#player{width:100%;height:100%}</style>
</head><body>
<div id="player"></div>
<script>
var t=document.createElement('script');
t.src='https://www.youtube.com/iframe_api';
document.head.appendChild(t);
function onYouTubeIframeAPIReady(){
  new YT.Player('player',{
    videoId:'$key',
    playerVars:{autoplay:1,rel:0,playsinline:1,modestbranding:1},
    events:{onError:function(e){if(window.AndroidBridge)AndroidBridge.onPlayerError(e.data)}}
  });
}
</script>
</body></html>"""
                        webViewPlayer.loadDataWithBaseURL(
                            "https://www.youtube.com", html, "text/html", "utf-8", null
                        )
                        isTrailerPlaying = true
                    }

                    webViewPlayer.addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onPlayerError(errorCode: Int) {
                            val key = trailerKey ?: return
                            this@MovieDetailActivity.runOnUiThread {
                                webViewPlayer.stopLoading()
                                webViewPlayer.visibility = android.view.View.GONE
                                openInYouTube(key)
                            }
                        }
                    }, "AndroidBridge")

                    val castAdapter = CastAdapter()
                    val watchProviderAdapter = WatchProviderAdapter()
                    rvCast.apply {
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                        adapter = castAdapter
                    }
                    rvWatchProviders.apply {
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                        adapter = watchProviderAdapter
                    }

                    ivBackdrop.setOnClickListener {
                        startTrailerPlayback()
                    }
                    btnPlayTrailer.setOnClickListener {
                        startTrailerPlayback()
                    }

                    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

                    var backdropProgressAnimator: ValueAnimator? = null
                    fun startBackdropProgress() {
                        backdropProgressAnimator?.cancel()
                        backdropLoadingOverlay.visibility = android.view.View.VISIBLE
                        backdropProgressAnimator = ValueAnimator.ofInt(0, 90).apply {
                            duration = 900
                            repeatCount = ValueAnimator.INFINITE
                            addUpdateListener {
                                tvBackdropProgress.text = "${it.animatedValue}%"
                            }
                            start()
                        }
                    }

                    fun finishBackdropProgress() {
                        backdropProgressAnimator?.cancel()
                        tvBackdropProgress.text = "100%"
                        backdropLoadingOverlay.postDelayed({
                            backdropLoadingOverlay.visibility = android.view.View.GONE
                        }, 120)
                    }

                    val imagePath = backdropPath ?: posterPath
                    val imageUrl = imagePath?.let { "https://image.tmdb.org/t/p/w780$it" }

                    startBackdropProgress()
                    ivBackdrop.load(imageUrl) {
                        crossfade(true)
                        placeholder(R.mipmap.ic_launcher)
                        error(R.mipmap.ic_launcher)
                        listener(
                            onSuccess = { _, _ -> finishBackdropProgress() },
                            onError = { _, _ -> finishBackdropProgress() }
                        )
                    }

                    ViewCompat.setOnApplyWindowInsetsListener(btnBack) { backView, insets ->
                        val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                        val params = backView.layoutParams as ViewGroup.MarginLayoutParams
                        params.topMargin = statusBarTop + 12
                        backView.layoutParams = params
                        insets
                    }

                    svDetail.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        ivBackdrop.translationY = scrollY * 0.35f
                        val fade = (1f - (scrollY / 500f)).coerceIn(0.55f, 1f)
                        ivBackdrop.alpha = fade
                    }

                    tvTitle.text = title
                    tvTagline.text = ""
                    tvOriginalTitle.text = title.ifBlank { getString(R.string.movie_unknown) }
                    tvStatus.text = getString(R.string.movie_unknown)
                    tvRatingInline.text = getString(R.string.movie_rating, rating)
                    tvRuntimeInline.text = getString(R.string.movie_runtime, getString(R.string.movie_unknown))
                    tvGenres.text = getString(R.string.movie_unknown)
                    tvRelease.text = releaseDate.ifBlank { getString(R.string.movie_unknown) }
                    tvBudget.text = getString(R.string.movie_unknown)
                    tvRevenue.text = getString(R.string.movie_unknown)
                    tvLanguage.text = language.ifBlank { getString(R.string.movie_unknown) }
                    tvPopularity.text = String.format(Locale.US, "%.1f", popularity)
                    tvVoteCount.text = "0"
                    tvCountries.text = getString(R.string.movie_unknown)
                    tvSpokenLanguages.text = getString(R.string.movie_unknown)
                    tvOriginCountry.text = getString(R.string.movie_unknown)
                    tvCollection.text = getString(R.string.movie_unknown)
                    tvProductionCompanies.text = getString(R.string.movie_unknown)
                    tvHomepage.text = getString(R.string.movie_unknown)
                    tvImdb.text = getString(R.string.movie_unknown)
                    tvOverview.text = overview.ifBlank { getString(R.string.movie_no_overview) }
                    tvCastEmpty.text = getString(R.string.movie_no_cast)
                    tvCrew.text = getString(R.string.movie_no_crew)
                    tvWatchProvidersMeta.text = ""
                    tvWatchProvidersEmpty.text = getString(R.string.movie_watch_providers_empty)
                    rvWatchProviders.visibility = android.view.View.GONE
                    webViewPlayer.visibility = android.view.View.GONE
                    webViewPlayer.isClickable = false
                    btnPlayTrailer.visibility = android.view.View.GONE
                    tvVideosTitle.visibility = android.view.View.GONE
                    tvVideos.text = getString(R.string.movie_videos_empty)

                    btnBack.setOnClickListener {
                        finish()
                    }

                    if (movieId > 0) {
                        lifecycleScope.launch {
                            repository.getMovieDetail(movieId = movieId, language = "en-US")
                                .onSuccess { detail ->
                                    Log.d(TAG, "Detail API result=$detail")

                                    val fullImagePath = detail.backdropPath ?: detail.posterPath
                                    val fullImageUrl = fullImagePath?.let { "https://image.tmdb.org/t/p/w780$it" }
                                    startBackdropProgress()
                                    ivBackdrop.load(fullImageUrl) {
                                        crossfade(true)
                                        placeholder(R.mipmap.ic_launcher)
                                        error(R.mipmap.ic_launcher)
                                        listener(
                                            onSuccess = { _, _ -> finishBackdropProgress() },
                                            onError = { _, _ -> finishBackdropProgress() }
                                        )
                                    }

                                    val genreText = detail.genres.joinToString { it.name }
                                        .ifBlank { getString(R.string.movie_unknown) }
                                    val runtimeText = detail.runtime?.let { "$it min" }
                                        ?: getString(R.string.movie_unknown)
                                    val countriesText = detail.productionCountries.joinToString { it.name }
                                        .ifBlank { getString(R.string.movie_unknown) }
                                    val originCountryText = detail.originCountry.joinToString()
                                        .ifBlank { getString(R.string.movie_unknown) }
                                    val languagesText = detail.spokenLanguages
                                        .joinToString { it.englishName.ifBlank { it.name } }
                                        .ifBlank { getString(R.string.movie_unknown) }
                                    val collectionText = detail.belongsToCollection?.name
                                        ?.ifBlank { getString(R.string.movie_unknown) }
                                        ?: getString(R.string.movie_unknown)
                                    val companiesText = detail.productionCompanies.joinToString { it.name }
                                        .ifBlank { getString(R.string.movie_unknown) }
                                    val budgetText = if (detail.budget > 0L) {
                                        currencyFormatter.format(detail.budget)
                                    } else {
                                        getString(R.string.movie_unknown)
                                    }
                                    val revenueText = if (detail.revenue > 0L) {
                                        currencyFormatter.format(detail.revenue)
                                    } else {
                                        getString(R.string.movie_unknown)
                                    }
                                    val homepageText = detail.homepage?.ifBlank { getString(R.string.movie_unknown) }
                                        ?: getString(R.string.movie_unknown)
                                    val imdbText = detail.imdbId?.ifBlank { getString(R.string.movie_unknown) }
                                        ?: getString(R.string.movie_unknown)

                                    tvTitle.text = detail.title
                                    tvTagline.text = detail.tagline?.takeIf { it.isNotBlank() }
                                        ?.let { getString(R.string.movie_tagline, it) }
                                        .orEmpty()
                                    tvOriginalTitle.text = detail.originalTitle.ifBlank { getString(R.string.movie_unknown) }
                                    tvStatus.text = detail.status?.ifBlank { getString(R.string.movie_unknown) }
                                        ?: getString(R.string.movie_unknown)
                                    tvRatingInline.text = getString(R.string.movie_rating, detail.voteAverage)
                                    tvRuntimeInline.text = getString(R.string.movie_runtime, runtimeText)
                                    tvGenres.text = genreText
                                    tvRelease.text = detail.releaseDate?.ifBlank { getString(R.string.movie_unknown) }
                                        ?: getString(R.string.movie_unknown)
                                    tvBudget.text = budgetText
                                    tvRevenue.text = revenueText
                                    tvLanguage.text = detail.originalLanguage.uppercase()
                                    tvPopularity.text = String.format(Locale.US, "%.1f", detail.popularity)
                                    tvVoteCount.text = detail.voteCount.toString()
                                    tvCountries.text = countriesText
                                    tvSpokenLanguages.text = languagesText
                                    tvOriginCountry.text = originCountryText
                                    tvCollection.text = collectionText
                                    tvProductionCompanies.text = companiesText
                                    tvHomepage.text = homepageText
                                    tvImdb.text = imdbText
                                    tvOverview.text = detail.overview.ifBlank { getString(R.string.movie_no_overview) }
                                }
                                .onFailure { error ->
                                    Log.d(TAG, "Detail API error=${error.message}", error)
                                }

                            repository.getMovieCredits(movieId = movieId, language = "en-US")
                                .onSuccess { credits ->
                                    Log.d(TAG, "Credits API result count=${credits.cast.size}")
                                    val sortedCast = credits.cast.sortedBy { it.order ?: Int.MAX_VALUE }.take(20)
                                    castAdapter.submitList(sortedCast)
                                    tvCastEmpty.text = if (sortedCast.isEmpty()) {
                                        getString(R.string.movie_no_cast)
                                    } else {
                                        ""
                                    }

                                    val highlightedCrew = credits.crew
                                        .filter { it.job != null && it.name.isNotBlank() }
                                        .take(8)
                                        .joinToString("\n") { crew ->
                                            "• ${crew.job}: ${crew.name}"
                                        }
                                    tvCrew.text = highlightedCrew.ifBlank { getString(R.string.movie_no_crew) }
                                }
                                .onFailure { error ->
                                    Log.d(TAG, "Credits API error=${error.message}", error)
                                }

                            repository.getMovieWatchProviders(movieId = movieId)
                                .onSuccess { watchProviders ->
                                    val regionCode = when {
                                        watchProviders.results.containsKey("ID") -> "ID"
                                        watchProviders.results.containsKey("US") -> "US"
                                        else -> watchProviders.results.keys.firstOrNull()
                                    }
                                    val regionData = regionCode?.let { watchProviders.results[it] }
                                    if (regionCode == null || regionData == null) {
                                        tvWatchProvidersMeta.text = ""
                                        tvWatchProvidersEmpty.text = getString(R.string.movie_watch_providers_empty)
                                        rvWatchProviders.visibility = android.view.View.GONE
                                    } else {
                                        val providers = (regionData.flatrate + regionData.rent + regionData.buy + regionData.ads)
                                            .distinctBy { it.providerId }
                                            .sortedBy { it.displayPriority }

                                        val metaLines = buildString {
                                            append(getString(R.string.movie_watch_providers_region, regionCode))
                                            regionData.link?.let {
                                                append("\n")
                                                append(getString(R.string.movie_watch_providers_link, it))
                                            }
                                        }
                                        tvWatchProvidersMeta.text = metaLines

                                        if (providers.isEmpty()) {
                                            tvWatchProvidersEmpty.text = getString(R.string.movie_watch_providers_empty)
                                            rvWatchProviders.visibility = android.view.View.GONE
                                        } else {
                                            watchProviderAdapter.submitList(providers)
                                            rvWatchProviders.visibility = android.view.View.VISIBLE
                                            tvWatchProvidersEmpty.text = ""
                                        }
                                    }
                                }
                                .onFailure { error ->
                                    Log.d(TAG, "Watch providers API error=${error.message}", error)
                                    tvWatchProvidersMeta.text = ""
                                    tvWatchProvidersEmpty.text = getString(R.string.movie_watch_providers_empty)
                                    rvWatchProviders.visibility = android.view.View.GONE
                                }

                            repository.getMovieVideos(movieId = movieId, language = "en-US")
                                .onSuccess { videos ->
                                    val youtubeVideos = videos.results
                                        .filter { it.site.equals("YouTube", ignoreCase = true) }
                                        .filter { it.key.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
                                        .sortedWith(
                                            compareByDescending<cloud.meis.data.model.MovieVideo> { it.official }
                                                .thenByDescending { it.type.equals("Trailer", ignoreCase = true) }
                                        )
                                        .take(5)

                                    if (youtubeVideos.isEmpty()) {
                                        trailerKey = null
                                        webViewPlayer.visibility = android.view.View.GONE
                                        btnPlayTrailer.visibility = android.view.View.GONE
                                        tvVideosTitle.visibility = android.view.View.GONE
                                        tvVideos.text = getString(R.string.movie_videos_empty)
                                    } else {
                                        val firstTrailer = youtubeVideos.first()
                                        trailerKey = firstTrailer.key
                                        btnPlayTrailer.visibility = android.view.View.VISIBLE
                                        startPulseAnimation()
                                        tvVideosTitle.visibility = android.view.View.VISIBLE
                                        val trailerUrl = "https://www.youtube.com/watch?v=${firstTrailer.key}"
                                        tvVideos.text = "${getString(R.string.movie_videos_playing, firstTrailer.name)}\n${getString(R.string.movie_trailer_link, trailerUrl)}"
                                    }
                                }
                                .onFailure { error ->
                                    Log.d(TAG, "Videos API error=${error.message}", error)
                                    trailerKey = null
                                    webViewPlayer.visibility = android.view.View.GONE
                                    btnPlayTrailer.visibility = android.view.View.GONE
                                    tvVideosTitle.visibility = android.view.View.GONE
                                    tvVideos.text = getString(R.string.movie_videos_empty)
                                }
                        }
                    }

                    view
                }
            )
        }
    }

}
