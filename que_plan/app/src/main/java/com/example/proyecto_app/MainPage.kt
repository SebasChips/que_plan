package com.example.proyecto_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
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
import androidx.core.view.GravityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore


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

    companion object {
        private const val REQUEST_CODE = 0
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        databaseRecover = FirebaseDatabase.getInstance().reference.child("locations")

        //logOut
        val bundle=intent.extras
        add = findViewById(R.id.add)

        //MAPS
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


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
        //logOut



        when(item.itemId){
            R.id.resena->resena()
            R.id.cuenta->account()
            R.id.nav_item_three->Toast.makeText(this, "Item3", Toast.LENGTH_SHORT).show()
            R.id.mapa-> mapa()
            R.id.logout-> logout()


        }
        drawer.closeDrawer(GravityCompat.START)
        return true
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
                            MarkerOptions().position(coordinates).title(idPlaces)
                        )
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
    private fun resena(){
        val intent = Intent(this, resenasActivity::class.java)
        startActivity(intent)

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


