package com.example.proyecto_app;

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LogIn : AppCompatActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var signinBtn: TextView
    private lateinit var googleSignInBtn: ImageView
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    //base de datos
    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)
        signinBtn = findViewById(R.id.signin_btn)
        googleSignInBtn = findViewById(R.id.login_btn_google)

        signinBtn.setOnClickListener {
            val intent = Intent(this@LogIn, SignInActivity::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            if (usernameInput.text.isNotEmpty() && passwordInput.text.isNotEmpty()) {
                auth.signInWithEmailAndPassword(
                    usernameInput.text.toString(),
                    passwordInput.text.toString()
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val email = it.result?.user?.email ?: ""
                        goToMainPage(email, "EMAIL")
                    }
                }
            }
        }

        googleSignInBtn.setOnClickListener {
            signInWithGoogle(it)
        }

        // Configura el inicio de sesión con Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    fun signInWithGoogle(view: View) {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }



    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val email = user?.email ?: ""
                val displayName = user?.displayName ?: ""
                val (firstName, lastName) = splitName(displayName)

                db.collection("users").document(email.toString()).set(
                    hashMapOf("username" to displayName.toString(),
                        "name" to firstName.toString(),
                        "lastname" to lastName.toString()))

                goToMainPage(email, "GOOGLE")

            }

        }
    }

    private fun splitName(fullName: String): Pair<String, String> {
        val names = fullName.split(" ")
        val firstName = names.firstOrNull() ?: ""
        val lastName = names.drop(1).joinToString(" ")
        return Pair(firstName, lastName)
    }





    private fun goToMainPage(email: String, provider: String) {
        val mainPageIntent = Intent(this, MainPage::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider)
        }
        startActivity(mainPageIntent)
        finish()
    }

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LogInActivity"
    }
}
