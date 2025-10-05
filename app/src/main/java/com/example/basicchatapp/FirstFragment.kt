package com.example.basicchatapp

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.basicchatapp.databinding.FragmentFirstBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class FirstFragment : Fragment() {

    private lateinit var recyclerUsers: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var userAdapter: UserAdapter
    private val users = mutableListOf<User>()

    private val db = Firebase.firestore
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerUsers = view.findViewById(R.id.recyclerUsers)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        
        // Setup RecyclerView
        recyclerUsers.layoutManager = LinearLayoutManager(requireContext())

        userAdapter = UserAdapter(users) { user ->
            // On click user → open chat
            val bundle = Bundle().apply {
                putString("receiverUid", user.uid)
            }
            findNavController().navigate(R.id.action_FirstFragment_to_ChatView, bundle)
        }
        recyclerUsers.adapter = userAdapter

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshUsers()
        }

        // Set refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        loadUsers()
    }

    private fun loadUsers() {
        // Set up real-time listener for users
        db.collection("users")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    users.clear()
                    for (document in snapshots.documents) {
                        val user = document.toObject(User::class.java)
                        if (user != null && user.uid != currentUser?.uid) {
                            users.add(user)
                        }
                    }
                    userAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun refreshUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                users.clear()
                for (doc in result) {
                    val user = doc.toObject(User::class.java)

                    if (user.uid != currentUser?.uid) {
                        users.add(user)
                    }
                }
                userAdapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error refreshing users: ", exception)
                swipeRefreshLayout.isRefreshing = false
            }
    }
}
