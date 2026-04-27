package cloud.meis.ui.movies

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.meis.R
import cloud.meis.data.model.Movie
import coil.load
import coil.request.CachePolicy

class MovieGridAdapter(
    private val onMovieClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieGridAdapter.MovieViewHolder>(MovieDiffCallback()) {

    private var lastAnimatedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
        return MovieViewHolder(view, onMovieClick)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))

        if (position > lastAnimatedPosition) {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 28f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(380)
                .setStartDelay((position * 28L).coerceAtMost(240L))
                .start()
            lastAnimatedPosition = position
        }
    }

    class MovieViewHolder(
        itemView: View,
        private val onMovieClick: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivPoster: ImageView = itemView.findViewById(R.id.ivPoster)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        private val loadingOverlay: LinearLayout = itemView.findViewById(R.id.loadingOverlay)
        private val tvImageProgress: TextView = itemView.findViewById(R.id.tvImageProgress)
        private var progressAnimator: ValueAnimator? = null

        private fun startFakeProgress() {
            progressAnimator?.cancel()
            loadingOverlay.visibility = View.VISIBLE
            progressAnimator = ValueAnimator.ofInt(0, 90).apply {
                duration = 900
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    val value = it.animatedValue as Int
                    tvImageProgress.text = "$value%"
                }
                start()
            }
        }

        private fun finishProgress() {
            progressAnimator?.cancel()
            tvImageProgress.text = "100%"
            loadingOverlay.postDelayed({ loadingOverlay.visibility = View.GONE }, 120)
        }

        fun bind(movie: Movie) {
            tvTitle.text = movie.title
            val year = movie.releaseDate?.take(4)?.ifBlank { null }
                ?: itemView.context.getString(R.string.movie_unknown_year)
            val language = movie.originalLanguage.uppercase()
            tvSubtitle.text = itemView.context.getString(R.string.movie_card_subtitle, year, language)
            tvRating.text = itemView.context.getString(R.string.movie_rating, movie.voteAverage)

            val posterUrl = movie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
            startFakeProgress()
            ivPoster.load(posterUrl) {
                crossfade(true)
                placeholder(R.mipmap.ic_launcher)
                error(R.mipmap.ic_launcher)
                memoryCachePolicy(CachePolicy.ENABLED)
                diskCachePolicy(CachePolicy.ENABLED)
                listener(
                    onSuccess = { _, _ -> finishProgress() },
                    onError = { _, _ -> finishProgress() }
                )
            }

            itemView.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    private class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
}
