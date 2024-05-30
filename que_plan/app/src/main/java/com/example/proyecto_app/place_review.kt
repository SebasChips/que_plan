package com.example.proyecto_app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.cardview.widget.CardView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class place_review : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    // MENU
    private lateinit var drawer: DrawerLayout
    private lateinit var toogle: ActionBarDrawerToggle
    private lateinit var reviewsLayout: LinearLayout
    private var markerTitle: String? = null
    private var markerTitleID: String? = null

    lateinit var namePlace : TextView

    var name: String? = null

    // Dialog
    private lateinit var dialog: AlertDialog
    lateinit var add: FloatingActionButton

    private lateinit var createReview: Button
    private lateinit var inputReview: EditText

    // Obtener el usuario actual
    // DB
    private val db = FirebaseFirestore.getInstance()
    val reviewsCollection = db.collection("reviews")
    val usersCollection = db.collection("users")
    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    val actualUser: String? = user?.email
    private lateinit var databaseRecover: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_review)

        createReview = findViewById(R.id.write_review_button)
        inputReview = findViewById(R.id.review_edit_text)
        reviewsLayout = findViewById(R.id.reviewsLayout)
        namePlace = findViewById(R.id.namePlace)
        markerTitleID =markerTitle

        markerTitle = intent.getStringExtra("marker_title")
        loadNamePhotosRating()
        loadReviews()

        //MENU
        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        toogle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toogle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val navigationView: NavigationView = findViewById(R.id.navView)
        navigationView.setNavigationItemSelectedListener(this)

        createReview.setOnClickListener {
            showAlertDialog { valoracion ->
                val reviewText = inputReview.text.toString()
                val reviewData = hashMapOf(
                    "email" to (actualUser ?: ""),
                    "reviews" to hashMapOf(
                        "placeId" to (markerTitle ?: ""),
                        "resena" to reviewText,
                        "valoracion" to valoracion
                    )
                )

                db.collection("reviews").add(reviewData)
                    .addOnSuccessListener {
                        recreate()
                    }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.resena -> {
                val intent = Intent(this, resenasActivity::class.java)
                startActivity(intent)
            }
            R.id.cuenta -> {
                val intent = Intent(this, place_review::class.java)
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

    private fun showAlertDialog(callback: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        val aceptar = dialogView.findViewById<Button>(R.id.botonAceptar)
        val valoracion: AppCompatSpinner = dialogView.findViewById(R.id.valoracion)

        // Inicializa el diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .show()

        // Configura el adaptador del Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.star_ratings,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            valoracion.adapter = adapter
        }

        // Establece el fondo del diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)

        // Configura el botón "Aceptar"
        aceptar.setOnClickListener {
            val elementoSeleccionado = valoracion.selectedItem.toString()
            callback(elementoSeleccionado)
            dialog.dismiss()
        }
    }

    private fun loadNamePhotosRating() {
        // markerTitle se asume que siempre es un string no nulo con el ID
        val documentReference = db.collection("places").document(markerTitle?:"")

        documentReference.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val name = documentSnapshot.getString("locations.nombre")
                namePlace.setText(name ?: "faak")
            } else {
                // Manejar el caso donde el documento no existe
                namePlace.setText("NO JALA EL DOCUMENTO")
            }
        }.addOnFailureListener { exception ->
            // Manejar el error en la obtención del documento
            Log.e("FirestoreError", "Error al obtener el documento", exception)
            namePlace.setText("NO JALA EL DOCUMENTO2") // En caso de error, usar markerTitle
        }
    }


    private fun loadReviews() {
        reviewsCollection.get()
            .addOnSuccessListener { result ->


                for (document in result) {
                    val locations = document.get("reviews") as? Map<String, Any>
                    val placeId = locations?.get("placeId") as? String
                    val valoracion = locations?.get("resena") as? String


                    if (placeId == markerTitle) {
                        val colorPink = ContextCompat.getColor(this, R.color.pink)
                        val colorPurple = ContextCompat.getColor(this, R.color.purple)
                        val colorBlack = ContextCompat.getColor(this, R.color.black)
                        val colorWhite = ContextCompat.getColor(this, R.color.white)


                        // Crear un CardView para contener la reseña y el usuario, con bordes redondeados y sombra
                        val cardView = CardView(this).apply {
                            radius = 16f
                            cardElevation = 8f
                            setCardBackgroundColor(colorPurple)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(16, 16, 16, 16)
                            }
                        }

                        // Crear un LinearLayout para el contenido dentro del CardView
                        val reviewLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16, 16, 16, 16)
                        }

                        // Crear un TextView para la reseña
                        val textViewReview = TextView(this).apply {
                            text = valoracion ?: "No hay valoración"
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                            setTypeface(null, Typeface.BOLD)
                            setTextColor(colorWhite)
                        }

                        // Crear un TextView para el usuario
                        val textViewUser = TextView(this).apply {
                            text = name ?: "Usuario test"
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                            setTypeface(null, Typeface.NORMAL)
                            setTextColor(colorWhite)
                        }

                        // Crear un ImageView para el icono del usuario
                        val imageView = ImageView(this).apply {
                            setImageResource(R.drawable.baseline_account_circle_24)
                            layoutParams = LinearLayout.LayoutParams(
                                48, 48
                            ).apply {
                                setMargins(0, 0, 8, 0)
                            }
                        }

                        // Crear un LinearLayout horizontal para el usuario y su icono
                        val userLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            addView(imageView)
                            addView(textViewUser)
                        }

                        // Crear una vista para la línea divisoria
                        val dividerView = View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1
                            ).apply {
                                setMargins(0, 16, 0, 16)
                            }
                            setBackgroundColor(colorBlack)
                        }

                        // Añadir vistas al layout de la reseña
                        reviewLayout.addView(textViewReview)
                        reviewLayout.addView(userLayout)
                        reviewLayout.addView(dividerView)

                        // Añadir el layout de la reseña al CardView
                        cardView.addView(reviewLayout)

                        // Añadir el CardView al layout principal
                        reviewsLayout.addView(cardView)
                    }


                }


            }
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        toogle.syncState()
    }
    private fun resena(){
        val intent = Intent(this, resenasActivity()::class.java)

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


        }

    }

}
