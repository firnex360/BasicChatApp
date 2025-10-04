package com.example.basicchatapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val users: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.userName)
        val emailText: TextView = view.findViewById(R.id.userEmail)
        val chatIcon: ImageView = view.findViewById(R.id.userChatIcon)

        fun bind(user: User) {
            // Trim long names
            val displayName = if (user.first.length > 15) {
                user.first.take(15) + "..."
            } else {
                user.first
            }

            nameText.text = displayName
            emailText.text = user.email

            // Handle click anywhere on the row (including chat icon)
            itemView.setOnClickListener { onClick(user) }
            chatIcon.setOnClickListener { onClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}
