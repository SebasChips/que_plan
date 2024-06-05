package com.example.proyecto_app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import kotlinx.coroutines.*
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.Toolbar

import androidx.drawerlayout.widget.DrawerLayout

import com.google.android.material.navigation.NavigationView

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.GravityCompat
import com.example.proyecto_app.databinding.ActivityMainPageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await


class MainPage : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {


    //Dialog
    private lateinit var dialog:AlertDialog
    lateinit var add : FloatingActionButton

    //DB
    private val db = FirebaseFirestore.getInstance()
    val placesCollection = db.collection("places")
    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private lateinit var databaseRecover: DatabaseReference


    //MENU
    private lateinit var drawer: DrawerLayout
    private lateinit var toogle: ActionBarDrawerToggle

    //MAPS
    private lateinit var map: GoogleMap
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var currentMarker: Marker? = null


    //Galeria
    private var imageUriList = mutableListOf<Uri>()


    companion object {
        private const val REQUEST_CODE = 0
        val IMAGE_REQUEST_CODE= 100

    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        //Guardado de datos sesión

        databaseRecover = FirebaseDatabase.getInstance().reference.child("locations")

        //logOut
        add = findViewById(R.id.add)

        //MAPS
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


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
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.resena->Toast.makeText(this, "PROXIMAMENTE", Toast.LENGTH_SHORT).show()
            R.id.cuenta->account()
            R.id.nav_item_three->Toast.makeText(this, "PROXIMAMENTE", Toast.LENGTH_SHORT).show()
            R.id.mapa-> mapa()
            R.id.logout-> logout()


        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val imageUri = data.clipData!!.getItemAt(i).uri
                        imageUriList.add(imageUri)
                    }
                }
            }
        }
    }


    private fun uploadImageToFirebase(imageUriList: MutableList<Uri>, documentID: String) {
        val storageReference = FirebaseStorage.getInstance().reference
        var uploadedCount = 0 // Contador para el número de imágenes subidas

        for (imageUri in imageUriList) {
            val fileReference = storageReference.child("$documentID/${System.currentTimeMillis()}.jpg")

            fileReference.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    uploadedCount++
                    if (uploadedCount == imageUriList.size) {
                        // Si todas las imágenes se han subido, mostrar mensaje de éxito
                        Toast.makeText(this, "Imagenes subidas", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    // Manejar errores de carga de imagen
                    Toast.makeText(this, "Error al cargar imagenes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Selecciona las imagenes"), IMAGE_REQUEST_CODE)
    }



    private fun loadMarkers() {
        val defaultLatitude = 0.0
        val defaultLongitude = 0.0
        var placesCoordinates: LatLng? = null
        var coordinates = placesCoordinates ?: LatLng(defaultLatitude, defaultLongitude)

        placesCollection.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    // Extrae los campos específicos de cada documento
                    val locations = document.get("locations") as? Map<String, Any>
                    val latitude = locations?.get("latitude") as? Double
                    val longitude = locations?.get("longitude") as? Double
                    val idPlaces = document.id

                    if (latitude != null && longitude != null) {
                        val coordinates = LatLng(latitude, longitude)
                        map.addMarker(
                            MarkerOptions().position(coordinates).title(idPlaces) .snippet("Tu texto aquí"))

                    } else {
                        println("Latitude o longitude son nulos para el documento con ID: $idPlaces")
                    }
                }
            }

    }


        private fun account(){
        intent = Intent(this, my_profiles::class.java)
        startActivity(intent)
    }

    private fun logout(){
        val prefs =getSharedPreferences("preference_session", Context.MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()

        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        toogle.syncState()
    }

    private fun mapa(){
        intent = Intent(this, MainPage()::class.java)

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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        loadMarkers()
        enableLocation()
        getLastLocation()

        map.setOnMarkerClickListener { marker ->
            val intent = Intent(this, place_review::class.java)
            intent.putExtra("marker_title", marker.title) // Aquí pasamos el título del marcador a la otra actividad
            startActivity(intent)
            true
        }

        //Agregar sitio nuevo
        map.setOnMapClickListener { latLng ->
            currentMarker?.remove()

            currentMarker = map.addMarker(MarkerOptions().position(latLng).title("Ubicación seleccionada"))

            map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

            val latitude1 = latLng.latitude
            val longitude1 = latLng.longitude

            add.setOnClickListener(){
                if (user != null && currentMarker != null) {
                    val email = (user.email)
                    showAlertDialog(latitude1,longitude1,email?:"")
                    currentMarker?.remove()

                }

            }
        }
    }

    private fun showAlertDialog(latitude:Double, longitude:Double, email:String){
        val dialogView = layoutInflater.inflate(R.layout.dialog,null)
        dialog = AlertDialog.Builder(this).setView(dialogView).show()
        val aceptar = dialogView.findViewById<Button>(R.id.botonAceptar)
        val cancelar = dialogView.findViewById<Button>(R.id.botonCancelar)
        val imagenes = dialogView.findViewById<Button>(R.id.imagenes)
        val nombre = dialogView.findViewById<EditText>(R.id.nombre)
        val resena = dialogView.findViewById<EditText>(R.id.resena)

        //SPINNER
        val valoracion: AppCompatSpinner = dialogView.findViewById(R.id.valoracion)
        ArrayAdapter.createFromResource(
            this,
            R.array.star_ratings,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
           valoracion.adapter = adapter
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.white)


        imagenes.setOnClickListener(){
            pickImageGallery()
        }


        aceptar.setOnClickListener(){
            val nombreText = nombre.text.toString()
            val resenaText = resena.text.toString()
            val elementoSeleccionado = valoracion.selectedItem.toString()

            db.collection("places").add(
                hashMapOf(
                    "email" to (email ?: ""),
                    "locations" to hashMapOf(
                        "nombre" to nombreText,
                        "latitude" to latitude,
                        "longitude" to longitude
                    )
                )
            ).addOnSuccessListener { documentReference ->
                val newDocumentId = documentReference.id
                db.collection("reviews").add(
                    hashMapOf(
                        "email" to (email ?: ""),
                        "reviews" to hashMapOf(
                            "placeId" to newDocumentId,
                            "resena" to resenaText,
                            "valoracion" to elementoSeleccionado,

                            )
                    )
                )
                uploadImageToFirebase(imageUriList, newDocumentId)

            }


            recreate()
            dialog.dismiss()
        }
        cancelar.setOnClickListener(){

            dialog.dismiss()
        }

    }

    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    val zoom = LatLng(latitude,longitude)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(zoom,11f), 4000, null)
                }
            }
    }


    // Permission Location Check
    private fun checkLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun enableLocation() {
        if (!::map.isInitialized) return
        if (checkLocationPermission()) {
            map.isMyLocationEnabled = true

        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "ACTIVA LA LOCALIZACIÓN DESDE LOS AJUSTES PARA QUE FUNCIONE LA APP", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                } else {
                    Toast.makeText(this, "ACTIVA LA LOCALIZACIÓN DESDE LOS AJUSTES Y ACEPTA LOS PERMISOS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return
        if (!checkLocationPermission()) {
            map.isMyLocationEnabled = false
            Toast.makeText(this, "ACTIVA LA LOCALIZACIÓN DESDE LOS AJUSTES Y ACEPTA LOS PERMISOS", Toast.LENGTH_SHORT).show()
        }
    }



}


