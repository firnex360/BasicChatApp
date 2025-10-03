package com.example.basicchatapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.firestore

class ChatView : Fragment() {

    private val db = Firebase.firestore
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var editMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var chatAdapter: ChatAdapter

    private val chatRoomId = "general" // a fixed chat room for now

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_view, container, false)

        // link UI components
        recyclerMessages = view.findViewById(R.id.recyclerMessages)
        editMessage = view.findViewById(R.id.editMessage)
        buttonSend = view.findViewById(R.id.buttonSend)

        // RecyclerView setup
        recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter(mutableListOf())
        recyclerMessages.adapter = chatAdapter

        // Send button click
        buttonSend.setOnClickListener {
            val text = editMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                editMessage.text.clear()
            }
        }

        // Start listening for new messages
        listenForMessages()

        return view
    }

    // Send a message to Firestore
    private fun sendMessage(text: String) {
        val message = hashMapOf(
            "text" to text,
            "sender" to "user123",   // later: replace with FirebaseAuth user
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                Log.d("ChatView", "Message sent!")
            }
            .addOnFailureListener { e ->
                Log.w("ChatView", "Error sending message", e)
            }
    }

    // Listen in real-time for new messages
    private fun listenForMessages() {
        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatView", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (docChange in snapshots.documentChanges) {
                        if (docChange.type == DocumentChange.Type.ADDED) {
                            val message = docChange.document.toObject(ChatMessage::class.java)
                            chatAdapter.addMessage(message)
                            recyclerMessages.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }
                }
            }
    }
}
