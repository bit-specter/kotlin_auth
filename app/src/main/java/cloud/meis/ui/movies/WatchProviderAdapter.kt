package cloud.meis.ui.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.meis.R
import cloud.meis.data.model.WatchProvider
import coil.load

class WatchProviderAdapter : ListAdapter<WatchProvider, WatchProviderAdapter.WatchProviderViewHolder>(
    WatchProviderDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchProviderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_watch_provider, parent, false)
        return WatchProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: WatchProviderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WatchProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProviderLogo: ImageView = itemView.findViewById(R.id.ivProviderLogo)
        private val tvProviderName: TextView = itemView.findViewById(R.id.tvProviderName)

        fun bind(provider: WatchProvider) {
            tvProviderName.text = provider.providerName
            val logoUrl = provider.logoPath?.let { "https://image.tmdb.org/t/p/w185$it" }
            ivProviderLogo.load(logoUrl) {
                crossfade(true)
                placeholder(R.mipmap.ic_launcher)
                error(R.mipmap.ic_launcher)
            }
            ivProviderLogo.contentDescription = itemView.context.getString(
                R.string.movie_watch_provider_logo_named,
                provider.providerName
            )
        }
    }

    private class WatchProviderDiffCallback : DiffUtil.ItemCallback<WatchProvider>() {
        override fun areItemsTheSame(oldItem: WatchProvider, newItem: WatchProvider): Boolean {
            return oldItem.providerId == newItem.providerId
        }

        override fun areContentsTheSame(oldItem: WatchProvider, newItem: WatchProvider): Boolean {
            return oldItem == newItem
        }
    }
}
