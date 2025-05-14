package com.example.taller3prueba1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class SeguimientoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var nombreUsuario: String? = null
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguimiento)

        nombreUsuario = intent.getStringExtra("nombre")
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapSeguimiento) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val ubicacion = LatLng(lat, lng)
        map.addMarker(MarkerOptions().position(ubicacion).title(nombreUsuario))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 16f))
    }
}