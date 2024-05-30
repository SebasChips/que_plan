package com.example.proyecto_app

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

enum class ProviderType {
    BASIC
}

class LogIn : AppCompatActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var signinBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)
        signinBtn = findViewById(R.id.signin_btn)

        signinBtn.setOnClickListener {
            val intent = Intent(this@LogIn, SignInActivity::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {

            if (usernameInput.text.isNotEmpty() && passwordInput.text.isNotEmpty())
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    usernameInput.text.toString(),
                    passwordInput.text.toString()
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        LogIn(it.result?.user?.email?:"", ProviderType.BASIC)
                    } else showAlert()
                }
        }


    }


    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Usuario/contraseña incorrecto")
        builder.setMessage("Verifique su contraseña y/o correo")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun LogIn(email: String, provider: ProviderType) {
        val mainPageIntent = Intent(this, MainPage::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(mainPageIntent)
    }
}
