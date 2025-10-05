package com.example.basicchatapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class ChatView : Fragment() {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val senderUid = currentUser?.uid

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var editMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonAttach: ImageButton
    private lateinit var chatAdapter: ChatAdapter

    private var receiverUid: String? = null
    private var chatRoomId: String? = null
    private val currentName = currentUser?.displayName ?: "You"

    private var isKeyboardShowing = false
    private var previousTitle: CharSequence? = null

    // Activity Result Launcher for image picking
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                uploadImageToStorage(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiverUid = arguments?.getString("receiverUid")

        if (senderUid != null && receiverUid != null) {
            chatRoomId = getChatRoomId(senderUid, receiverUid!!)
        } else {
            Log.e("ChatView", "Missing sender or receiver UID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_view, container, false)

        recyclerMessages = view.findViewById(R.id.recyclerMessages)
        editMessage = view.findViewById(R.id.editMessage)
        buttonSend = view.findViewById(R.id.buttonSend)
        buttonAttach = view.findViewById(R.id.buttonAttach)

        recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter(mutableListOf())
        recyclerMessages.adapter = chatAdapter

        buttonSend.setOnClickListener {
            val text = editMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                editMessage.text.clear()
            }
        }

        // Open gallery when pressing attach button
        buttonAttach.setOnClickListener {
            openImagePicker()
        }

        listenForMessages()
        setupKeyboardDetection(view)

        // Toolbar title handling (this was for testing)
        previousTitle = (activity as AppCompatActivity).supportActionBar?.title
        (activity as AppCompatActivity).supportActionBar?.title = "Unknown"

        receiverUid?.let { uid ->
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val fullName = doc.getString("first") ?: "Unknown"
                    val displayName = if (fullName.length > 15) fullName.take(15) + "…" else fullName
                    (activity as AppCompatActivity).supportActionBar?.title = displayName
                }
                .addOnFailureListener { e ->
                    Log.w("ChatView", "Could not load receiver name", e)
                }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as AppCompatActivity).supportActionBar?.title = "Chats"
    }

    // Send a message (text only)
    private fun sendMessage(text: String, imageUrl: String? = null) {
        if (chatRoomId == null || senderUid == null || receiverUid == null) {
            Log.w("ChatView", "Missing chatRoomId, senderUid or receiverUid")
            return
        }

        val message = hashMapOf(
            "text" to text,
            "sender" to senderUid,
            "receiver" to receiverUid,
            "senderName" to currentName,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatRoomId!!)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                Log.d("ChatView", "Message sent to room: $chatRoomId")
            }
            .addOnFailureListener { e ->
                Log.w("ChatView", "Error sending message", e)
            }
    }

    // Image Picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    // Upload to Firebase Storage
    private fun uploadImageToStorage(imageUri: Uri) {
        if (senderUid == null || receiverUid == null) return

        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference
            .child("chat_images")
            .child(chatRoomId!!)
            .child(fileName)

        val uploadTask = storageRef.putFile(imageUri)
        uploadTask
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    sendMessage("[Image]", uri.toString()) // send as image message
                    Log.d("ChatView", "Image uploaded and message sent.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatView", "Failed to upload image: ${e.message}")
            }
    }

    private fun listenForMessages() {
        if (chatRoomId == null) return

        db.collection("chats")
            .document(chatRoomId!!)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatView", "Listen failed.", e)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { docChange ->
                    if (docChange.type == DocumentChange.Type.ADDED) {
                        val message = docChange.document.toObject(ChatMessage::class.java)
                        chatAdapter.addMessage(message)
                        recyclerMessages.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
    }

    private fun setupKeyboardDetection(rootView: View) {
        val activityRootView = requireActivity().findViewById<View>(android.R.id.content)
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val heightDiff = activityRootView.rootView.height - activityRootView.height
                val isKeyboardOpen = heightDiff > activityRootView.rootView.height * 0.15

                if (isKeyboardOpen && !isKeyboardShowing) {
                    isKeyboardShowing = true
                    recyclerMessages.post {
                        if (chatAdapter.itemCount > 0)
                            recyclerMessages.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                } else if (!isKeyboardOpen && isKeyboardShowing) {
                    isKeyboardShowing = false
                }
            }
        })
    }
}

// Generate unique chat room ID (always same for same 2 users)
fun getChatRoomId(user1: String, user2: String): String {
    return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
}
