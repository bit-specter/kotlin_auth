package cloud.meis.ui.main

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.meis.R
import cloud.meis.data.local.entity.ServerEntity
import java.text.DateFormat
import java.util.Date

class ServerAdapter(
    private val onDeleteClick: (ServerEntity) -> Unit
) : ListAdapter<ServerEntity, ServerAdapter.ServerViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_server, parent, false)
        return ServerViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ServerViewHolder(
        itemView: View,
        private val onDeleteClick: (ServerEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val statusIndicator: View = itemView.findViewById(R.id.viewStatusIndicator)
        private val serverName: TextView = itemView.findViewById(R.id.tvServerName)
        private val hostAddress: TextView = itemView.findViewById(R.id.tvHostAddress)
        private val status: TextView = itemView.findViewById(R.id.tvStatus)
        private val latency: TextView = itemView.findViewById(R.id.tvLatency)
        private val lastChecked: TextView = itemView.findViewById(R.id.tvLastChecked)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteServer)

        fun bind(server: ServerEntity) {
            val context = itemView.context
            val isUnknown = server.lastChecked == 0L
            val colorRes = when {
                isUnknown -> R.color.status_unknown
                server.lastStatus -> R.color.status_up
                else -> R.color.status_down
            }

            serverName.text = server.serverName
            hostAddress.text = server.hostAddress
            status.text = when {
                isUnknown -> context.getString(R.string.status_unknown)
                server.lastStatus -> context.getString(R.string.status_up)
                else -> context.getString(R.string.status_down)
            }
            latency.text = if (isUnknown) {
                context.getString(R.string.latency_empty)
            } else {
                "${server.lastLatency} ms"
            }
            lastChecked.text = if (isUnknown) {
                context.getString(R.string.last_checked_empty)
            } else {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(server.lastChecked))
            }
            statusIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, colorRes)
            )
            deleteButton.setOnClickListener {
                onDeleteClick(server)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ServerEntity>() {
        override fun areItemsTheSame(oldItem: ServerEntity, newItem: ServerEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ServerEntity, newItem: ServerEntity): Boolean {
            return oldItem == newItem
        }
    }
}
