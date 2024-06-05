package com.example.proyecto_app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
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
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage




class place_review : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    // MENU
    private lateinit var drawer: DrawerLayout
    private lateinit var toogle: ActionBarDrawerToggle
    private lateinit var reviewsLayout: LinearLayout
    private var markerTitle: String? = null
    private var markerTitleID: String? = null

    var countReviews : Int? = 0
    var numberOfIterations = 0

    // Firebase Storage
    val storage = Firebase.storage
    val storageRef = storage.reference



    lateinit var namePlace : TextView
//Slider imagenes
private lateinit var viewFlipper: ViewFlipper


    // Dialog
    private lateinit var dialog: AlertDialog
    lateinit var add: FloatingActionButton

    private lateinit var createReview: Button
    private lateinit var inputReview: EditText


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

        viewFlipper = findViewById(R.id.viewFlipper)
        createReview = findViewById(R.id.write_review_button)
        inputReview = findViewById(R.id.review_edit_text)
        reviewsLayout = findViewById(R.id.reviewsLayout)
        namePlace = findViewById(R.id.namePlace)
        markerTitleID =markerTitle

        //animacion
        viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
        viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)


        markerTitle = intent.getStringExtra("marker_title")
        loadNamePhotosRating()
        loadReviews()
        loadPhotos()

        //MENU
        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        supportActionBar?.setTitle("")

        drawer = findViewById(R.id.drawer_layout)

        toogle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawer.addDrawerListener(toogle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        toogle.syncState()
        supportActionBar?.setHomeAsUpIndicator(R.drawable.hamburguer)

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
                val intent = Intent(this, my_profiles::class.java)
                startActivity(intent)
            }
            R.id.nav_item_three ->
                Toast.makeText(this, "PROXIMAMENTE", Toast.LENGTH_SHORT).show()
            R.id.logout -> logOut()
            R.id.mapa -> mapa()
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }



    private fun loadPhotos() {
        val documentsRef = storageRef.child(markerTitle + "/")
        documentsRef.listAll().addOnSuccessListener { listResult ->
            listResult.items.forEach { item ->
                // Descargar la imagen y mostrarla en el ViewFlipper
                item.downloadUrl.addOnSuccessListener { uri ->
                    val imageView = ImageView(this)
                    Glide.with(this)
                        .load(uri)
                        .fitCenter()
                        .into(imageView)
                    viewFlipper.addView(imageView)
                }.addOnFailureListener { exception ->
                    // Manejar errores al descargar la imagen
                }
            }
        }.addOnFailureListener { exception ->
            // Manejar errores al obtener la lista de archivos
        }
    }






    private fun showAlertDialog(callback: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        val aceptar = dialogView.findViewById<Button>(R.id.botonAceptar)
        val valoracion: AppCompatSpinner = dialogView.findViewById(R.id.valoracion)

        // Inicializa el diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .show()

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
    private fun loadReviews() {
        reviewsCollection.get()
            .addOnSuccessListener { result ->

                for (document in result) {
                    val locations = document.get("reviews") as? Map<String, Any>
                    val placeId = locations?.get("placeId") as? String
                    val valoracion = locations?.get("resena") as? String
                    val estrellas = locations?.get("valoracion") as? String
                    var estrellasCount = estrellas?.toInt()

                    val email = document.getString("email") ?: ""


                    if (placeId == markerTitle) {
                        numberOfIterations += 1
                        countReviews = estrellasCount?.let { countReviews?.plus(it) }
                        db.collection("users").document(email).get()
                            .addOnSuccessListener { userDoc ->
                                val username =
                                    userDoc.get("username") as? String ?: "Usuario no encontrado"

                                val colorPurple = ContextCompat.getColor(this, R.color.purple)
                                val colorBlack = ContextCompat.getColor(this, R.color.black)
                                val colorWhite = ContextCompat.getColor(this, R.color.white)

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
                                val reviewLayout = LinearLayout(this).apply {
                                    orientation = LinearLayout.VERTICAL
                                    setPadding(16, 16, 16, 16)
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        setMargins(0, 0, 0, 20)
                                    }
                                }

                                val textViewReview = TextView(this).apply {
                                    text = valoracion ?: "No hay valoración"
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                                    setTypeface(null, Typeface.BOLD)
                                    setTextColor(colorWhite)
                                }

                                val textViewUser = TextView(this).apply {
                                    text = username
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                                    setTypeface(null, Typeface.NORMAL)
                                    setTextColor(colorWhite)
                                }

                                val imageViewUser = ImageView(this).apply {
                                    setImageResource(R.drawable.baseline_account_circle_24)
                                    layoutParams = LinearLayout.LayoutParams(
                                        48, 48
                                    ).apply {
                                        setMargins(0, 0, 8, 0)
                                    }
                                }

                                val userLayout = LinearLayout(this).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = Gravity.CENTER_VERTICAL
                                    addView(imageViewUser)
                                    addView(textViewUser)
                                }

                                val dividerView = View(this).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        1
                                    ).apply {
                                        setMargins(0, 16, 0, 16)
                                    }
                                    setBackgroundColor(colorBlack)
                                }

                                reviewLayout.addView(textViewReview)
                                reviewLayout.addView(userLayout)
                                reviewLayout.addView(dividerView)

                                cardView.addView(reviewLayout)
                                reviewsLayout.addView(
                                    cardView,
                                    reviewsLayout.indexOfChild(inputReview)
                                )
                            }
                    }
                }
                //Promediar calificacion
                countReviews = countReviews?.div(numberOfIterations)

                val black = ContextCompat.getColor(this, R.color.black)
                val textValoracion = TextView(this).apply {
                    text = "Valoración"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24F) // Tamaño de texto aumentado a 24sp
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(black)
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 20, 0,0) // Aumento de los márgenes inferiores para separación
                    }
                }

                val namePlaceIndex = reviewsLayout.indexOfChild(findViewById(R.id.viewFlipper))

                // Insertar textValoracion justo debajo de namePlace
                reviewsLayout.addView(textValoracion, namePlaceIndex + 1)

                val imageViewEnd = ImageView(this).apply {
                    when(countReviews){
                        1->setImageResource(R.drawable.stari)
                            2->setImageResource(R.drawable.starii)
                                3->setImageResource(R.drawable.stariii)
                                    4->setImageResource(R.drawable.stariv)
                                        5->setImageResource(R.drawable.starv)
                        else -> {setImageResource(R.drawable.anadir1)}
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        300,
                        300
                    ).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }

                // Insertar imageViewEnd justo debajo de textValoracion
                reviewsLayout.addView(imageViewEnd, namePlaceIndex + 2)
            }
    }

    private fun loadNamePhotosRating() {
        // markerTitle se asume que siempre es un string no nulo con el ID
        val documentReference = db.collection("places").document(markerTitle?:"")

        documentReference.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val name = documentSnapshot.getString("locations.nombre")
                namePlace.setText(name ?: "unknown")
            } else {
                namePlace.setText("###############")
            }
        }.addOnFailureListener { exception ->
            // Manejar el error en la obtención del documento
            Log.e("FirestoreError", "Error al obtener el documento", exception)
            namePlace.setText("###############")
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
    // Función para cambiar a la vista anterior
    fun previousView(view: View) {
        viewFlipper.showPrevious()
    }

    // Función para cambiar a la vista siguiente
    fun nextView(view: View) {
        viewFlipper.showNext()
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

}
