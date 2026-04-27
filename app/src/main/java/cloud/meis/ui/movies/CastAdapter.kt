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
import cloud.meis.data.model.CastMember
import coil.load
import coil.request.CachePolicy

class CastAdapter : ListAdapter<CastMember, CastAdapter.CastViewHolder>(CastDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cast_member, parent, false)
        return CastViewHolder(view)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCharacter: TextView = itemView.findViewById(R.id.tvCharacter)
        private val loadingOverlay: LinearLayout = itemView.findViewById(R.id.loadingOverlay)
        private val tvImageProgress: TextView = itemView.findViewById(R.id.tvImageProgress)
        private var progressAnimator: ValueAnimator? = null

        private fun startFakeProgress() {
            progressAnimator?.cancel()
            loadingOverlay.visibility = View.VISIBLE
            progressAnimator = ValueAnimator.ofInt(0, 90).apply {
                duration = 850
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    tvImageProgress.text = "${it.animatedValue}%"
                }
                start()
            }
        }

        private fun finishProgress() {
            progressAnimator?.cancel()
            tvImageProgress.text = "100%"
            loadingOverlay.postDelayed({ loadingOverlay.visibility = View.GONE }, 120)
        }

        fun bind(cast: CastMember) {
            tvName.text = cast.name
            tvCharacter.text = cast.character?.ifBlank {
                itemView.context.getString(R.string.movie_cast_unknown_character)
            } ?: itemView.context.getString(R.string.movie_cast_unknown_character)

            val profileUrl = cast.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
            startFakeProgress()
            ivProfile.load(profileUrl) {
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
        }
    }

    private class CastDiffCallback : DiffUtil.ItemCallback<CastMember>() {
        override fun areItemsTheSame(oldItem: CastMember, newItem: CastMember): Boolean {
            return oldItem.id == newItem.id && oldItem.character == newItem.character
        }

        override fun areContentsTheSame(oldItem: CastMember, newItem: CastMember): Boolean {
            return oldItem == newItem
        }
    }
}
