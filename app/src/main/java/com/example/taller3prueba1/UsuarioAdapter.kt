package com.example.taller3prueba1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng

class UsuarioAdapter(
    private val usuarios: List<Usuario>,
    private val onItemClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgUsuario: ImageView = itemView.findViewById(R.id.imgUsuario)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombreUsuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]

        holder.txtNombre.text = usuario.nombre

        Glide.with(holder.itemView.context)
            .load(usuario.fotoUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .circleCrop()
            .into(holder.imgUsuario)

        holder.itemView.setOnClickListener {
            onItemClick(usuario) // Ahora se envía el objeto completo
        }
    }

    override fun getItemCount(): Int = usuarios.size
}