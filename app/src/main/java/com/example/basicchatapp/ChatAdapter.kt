package com.example.basicchatapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide


class ChatAdapter(val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSenderName: TextView = itemView.findViewById(R.id.textSenderNameSent)
        val textMessage: TextView = itemView.findViewById(R.id.textMessageSent)
        val textTimestamp: TextView = itemView.findViewById(R.id.textTimestampSent)
        val imageMessage: ImageView = itemView.findViewById(R.id.imageMessageSent)
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSenderName: TextView = itemView.findViewById(R.id.textSenderNameReceived)
        val textMessage: TextView = itemView.findViewById(R.id.textMessageReceived)
        val textTimestamp: TextView = itemView.findViewById(R.id.textTimestampReceived)
        val imageMessage: ImageView = itemView.findViewById(R.id.imageMessageReceived)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.sender == currentUserUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val formattedTime = timeFormatter.format(Date(message.timestamp))
        val name = message.senderName?.let { shortenName(it) }
            ?: if (message.sender == currentUserUid) "You" else "Unknown"

        when (holder) {
            is SentMessageViewHolder -> {
                holder.textSenderName.text = name
                holder.textTimestamp.text = formattedTime

                if (!message.imageUrl.isNullOrEmpty()) {
                    // Show image message
                    holder.imageMessage.visibility = View.VISIBLE
                    holder.textMessage.visibility = View.GONE

                    Glide.with(holder.itemView.context)
                        .load(message.imageUrl)
                        .centerCrop()
                        .into(holder.imageMessage)
                } else {
                    // Show text message
                    holder.imageMessage.visibility = View.GONE
                    holder.textMessage.visibility = View.VISIBLE
                    holder.textMessage.text = message.text
                }
            }

            is ReceivedMessageViewHolder -> {
                holder.textSenderName.text = name
                holder.textTimestamp.text = formattedTime

                if (!message.imageUrl.isNullOrEmpty()) {
                    holder.imageMessage.visibility = View.VISIBLE
                    holder.textMessage.visibility = View.GONE

                    Glide.with(holder.itemView.context)
                        .load(message.imageUrl)
                        .centerCrop()
                        .into(holder.imageMessage)
                } else {
                    holder.imageMessage.visibility = View.GONE
                    holder.textMessage.visibility = View.VISIBLE
                    holder.textMessage.text = message.text
                }
            }
        }
    }


    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    private fun shortenName(name: String): String {
        return if (name.length > 10) name.take(10) + "…" else name
    }
}
