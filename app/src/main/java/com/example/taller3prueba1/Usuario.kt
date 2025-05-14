package com.example.taller3prueba1

import com.google.android.gms.maps.model.LatLng

data class Usuario(
    val nombre: String,
    val fotoUrl: String?,
    val ubicacion: LatLng
)