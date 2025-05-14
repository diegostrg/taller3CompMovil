package com.example.taller3prueba1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Botón: centrar ubicación
        findViewById<FloatingActionButton>(R.id.fabCenterLocation).setOnClickListener {
            currentLatLng?.let {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16f))
            } ?: Toast.makeText(this, "Ubicación aún no disponible", Toast.LENGTH_SHORT).show()
        }

        // Botón: ver usuarios disponibles
        findViewById<FloatingActionButton>(R.id.fabVerUsuarios).setOnClickListener {
            mostrarListaUsuariosDisponibles()
        }

        // Botón: cambiar foto
        findViewById<Button>(R.id.btnCambiarFoto).setOnClickListener {
            abrirGaleria()
        }

        // Botón: cambiar estado
        findViewById<Button>(R.id.btnCambiarEstado).setOnClickListener {
            toggleEstadoUsuario()
        }

        // Botón: cerrar sesión
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // ⭐ Crear canal de notificaciones
        crearCanalNotificaciones()

        // Servicio que escucha usuarios disponibles
        crearCanalNotificaciones()
        escucharUsuariosDisponibles()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }

        map.isMyLocationEnabled = true

        val locationRequest = LocationRequest.Builder(1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    map.addMarker(MarkerOptions().position(currentLatLng!!).title("Tu ubicación"))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 16f))
                    guardarUbicacionEnFirebase(location)
                    fusedLocationClient.removeLocationUpdates(this)
                } else {
                    Toast.makeText(this@MainActivity, "Ubicación aún no disponible", Toast.LENGTH_SHORT).show()
                }
            }
        }, mainLooper)

        val locations = loadLocationsFromJSON()
        locations.forEach {
            val position = LatLng(it.latitude, it.longitude)
            map.addMarker(MarkerOptions().position(position).title(it.name))
        }

    }

    private fun guardarUbicacionEnFirebase(location: Location) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        userRef.child("latitud").setValue(location.latitude)
        userRef.child("longitud").setValue(location.longitude)
    }


    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "usuarios_disponibles",
                "Usuarios disponibles",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando un usuario se conecta"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }


    private fun loadLocationsFromJSON(): List<LocationPoint> {
        val json = assets.open("locations.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        val jsonArray = jsonObject.getJSONArray("locationsArray")

        val locations = mutableListOf<LocationPoint>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val lat = item.getDouble("latitude")
            val lng = item.getDouble("longitude")
            val name = item.getString("name")
            locations.add(LocationPoint(lat, lng, name))
        }
        return locations
    }

    private fun escucharUsuariosDisponibles() {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")

        usuariosRef.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val estado = snapshot.child("estado").getValue(String::class.java)
                val nombre = snapshot.child("nombre").getValue(String::class.java)
                val lat = snapshot.child("latitud").getValue(Double::class.java)
                val lng = snapshot.child("longitud").getValue(Double::class.java)

                if (estado == "disponible" && nombre != null && lat != null && lng != null) {
                    mostrarNotificacionUsuarioDisponible(nombre, lat, lng)
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarNotificacionUsuarioDisponible(nombre: String, lat: Double, lng: Double) {
        val intent = if (FirebaseAuth.getInstance().currentUser != null) {
            Intent(this, SeguimientoActivity::class.java).apply {
                putExtra("nombre", nombre)
                putExtra("lat", lat)
                putExtra("lng", lng)
            }
        } else {
            Intent(this, LoginActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "usuarios_disponibles")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Puedes cambiar el ícono si quieres
            .setContentTitle("Nuevo usuario disponible")
            .setContentText("$nombre se conectó")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun mostrarListaUsuariosDisponibles() {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")

        usuariosRef.get().addOnSuccessListener { snapshot ->
            val listaUsuarios = mutableListOf<Usuario>()

            for (usuarioSnap in snapshot.children) {
                val estado = usuarioSnap.child("estado").getValue(String::class.java)
                if (estado == "disponible") {
                    val nombre = usuarioSnap.child("nombre").getValue(String::class.java)
                    val foto = usuarioSnap.child("fotoPerfil").getValue(String::class.java)
                    val lat = usuarioSnap.child("latitud").getValue(Double::class.java)
                    val lng = usuarioSnap.child("longitud").getValue(Double::class.java)

                    if (nombre != null && lat != null && lng != null) {
                        listaUsuarios.add(
                            Usuario(nombre, foto, LatLng(lat, lng))
                        )
                    }
                }
            }

            if (listaUsuarios.isNotEmpty()) {
                val view = layoutInflater.inflate(R.layout.bottomsheet_usuarios, null)
                val recycler = view.findViewById<RecyclerView>(R.id.recyclerUsuarios)
                recycler.layoutManager = LinearLayoutManager(this)

                // 💙 Mostrar marcador azul al hacer clic
                recycler.adapter = UsuarioAdapter(listaUsuarios) { usuario ->
                    val markerOptions = MarkerOptions()
                        .position(usuario.ubicacion)
                        .title(usuario.nombre)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

                    map.addMarker(markerOptions)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(usuario.ubicacion, 16f))
                }

                val bottomSheet = BottomSheetDialog(this)
                bottomSheet.setContentView(view)
                bottomSheet.show()
            } else {
                Toast.makeText(this, "No hay usuarios disponibles", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
        }
    }


    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                subirImagenAFirebase(uri)
            }
        }


    private fun abrirGaleria() {
        seleccionarImagenLauncher.launch("image/*")
    }

    private fun subirImagenAFirebase(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().getReference("fotos_perfil/$uid.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
                    userRef.child("fotoPerfil").setValue(downloadUri.toString())
                    Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_estado -> {
                toggleEstadoUsuario()
                true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleEstadoUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)

        userRef.child("estado").get().addOnSuccessListener { snapshot ->
            val currentState = snapshot.value?.toString() ?: "desconectado"
            val nuevoEstado = if (currentState == "disponible") "desconectado" else "disponible"
            userRef.child("estado").setValue(nuevoEstado)
            Toast.makeText(this, "Estado cambiado a $nuevoEstado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}