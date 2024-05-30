package com.example.proyecto_app


import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SignInActivity : AppCompatActivity() {
    lateinit var loginAct : TextView
    lateinit var signinButton : TextView
    lateinit var nameSignIn : EditText
    lateinit var lastname : EditText
    lateinit var emailSignin : EditText
    lateinit var username : EditText
    lateinit var passwordSigin : EditText



    //base de datos
    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)


        loginAct =  findViewById<TextView>(R.id.loginActivity)
        signinButton =  findViewById<TextView>(R.id.signinButton)


        loginAct.setOnClickListener {
            val intent = Intent(this@SignInActivity, LogIn::class.java)
            startActivity(intent)
        }

        loginAct =  findViewById<TextView>(R.id.loginActivity)
        signinButton =  findViewById(R.id.signinButton)
        username =  findViewById(R.id.username)
        nameSignIn = findViewById(R.id.nameSignin)
        lastname = findViewById(R.id.lastnameSignin)
        emailSignin = findViewById(R.id.emailSignin)
        passwordSigin = findViewById(R.id.passwordSigin)


        signinButton.setOnClickListener {

            if(emailSignin.text.isNotEmpty() && passwordSigin.text.isNotEmpty() && nameSignIn.text.isNotEmpty() && lastname.text.isNotEmpty() && username.text.isNotEmpty() )
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailSignin.text.toString(),passwordSigin.text.toString()).addOnCompleteListener{
                    if(it.isSuccessful){
                        db.collection("users").document(emailSignin.text.toString()).set(
                            hashMapOf("username" to username.text.toString(),
                                "name" to nameSignIn.text.toString(),
                                "lastname" to lastname.text.toString()))
                        LogIn(it.result?.user?.email?:"", ProviderType.BASIC)
                    }else showAlert()
                }

        }



    }
    private fun showAlert(){
        val builder=AlertDialog.Builder(this)
        builder.setTitle("ERROR")
        builder.setMessage("Se ha producido un error autenticando al usuario")
        builder.setPositiveButton("Aceptar",null)
        val dialog:AlertDialog= builder.create()
        dialog.show()
    }
    private fun LogIn(email:String, provider: ProviderType){
        val mainPageintent = Intent(this, MainPage::class.java).apply {
            putExtra("email", email)
        putExtra("provider",provider.name)}
        startActivity(mainPageintent)
    }
}
