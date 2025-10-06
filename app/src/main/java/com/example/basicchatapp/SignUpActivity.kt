package com.example.basicchatapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.basicchatapp.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private val db = Firebase.firestore



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.button.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            val userName = binding.userName.text.toString()


            if(email.isNotEmpty() && password.isNotEmpty() && userName.isNotEmpty()){
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                    if(it.isSuccessful){
                        val intent = Intent(this, SignInActivity::class.java)
                        startActivity(intent)

                        //to save the user
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val user = hashMapOf(
                                "uid" to currentUser.uid,
                                "first" to (userName.ifEmpty { currentUser.email?.split("@")?.get(0) ?: "Unknown" }),
                                "email" to currentUser.email,
                                "fcmToken" to "" // Initialize empty, will be set when user opens MainActivity
                            )
                            db.collection("users").document(currentUser.uid).set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error creating user profile: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }

                    }else{
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
        }

        binding.loginButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

    }
}