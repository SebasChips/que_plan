package com.example.proyecto_app

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.File


class my_profiles : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    //MENU
    private lateinit var drawer: DrawerLayout
    private lateinit var toogle: ActionBarDrawerToggle
    lateinit var name : TextView
    lateinit var lastname : TextView
    lateinit var email : TextView
    lateinit var editButton : TextView


    // Obtener el usuario actual
    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser



    //base de datos
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_profile)

        //BuscarView
        name = findViewById(R.id.nameTextViewValue)
        lastname = findViewById(R.id.lastnameTextViewValue)
        email =findViewById(R.id.emailTextViewValue)
        editButton = findViewById(R.id.editProfileButton)

        recuperarUsuario()


        //MENU
        val toolbar: Toolbar= findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)

        toogle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,R.string.navigation_drawer_close)
        drawer.addDrawerListener(toogle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val navigationView:NavigationView=findViewById(R.id.navView)
        navigationView.setNavigationItemSelectedListener(this)

        }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.resena -> {
                val intent = Intent(this, resenasActivity::class.java)
                startActivity(intent)
            }
            R.id.cuenta -> {
                val intent = Intent(this, my_profiles::class.java)
                startActivity(intent)
            }
            R.id.nav_item_three ->
                Toast.makeText(this, "Item3", Toast.LENGTH_SHORT).show()

            R.id.logout -> logOut()

            R.id.mapa -> mapa()

        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        toogle.syncState()
    }
    private fun resena(){
        val intent = Intent(this@my_profiles, resenasActivity()::class.java)

    }
    private fun mapa(){
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)

    }

    private fun logOut(){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toogle.onConfigurationChanged(newConfig)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toogle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)

    }
    private fun recuperarUsuario() {


        // Verificar si el usuario está autenticado
        if (user != null) {
            // Obtener el correo electrónico del usuario
            val emailNew = (user.email)

            db.collection("users").document(emailNew ?: "").get().addOnSuccessListener {
                name.setText(it.get("name") as String?)
                lastname.setText(it.get("lastname") as String?)
                email.setText(emailNew)

            }
        }

    }

}
