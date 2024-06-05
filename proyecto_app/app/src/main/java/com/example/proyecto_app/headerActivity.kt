package com.example.proyecto_app;


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.example.proyecto_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore



class headerActivity : AppCompatActivity() {
    lateinit var username : TextView
    // Obtener el usuario actual
    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    //base de datos
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_profile)

        //BuscarView
        username = findViewById(R.id.usernameChange)


        recuperarUsuario()
    }




    private fun recuperarUsuario() {
        val email = (user?.email)
        db.collection("users").document(email ?: "").get().addOnSuccessListener {
            username.setText(it.get("username") as String?)

        }

    }
}