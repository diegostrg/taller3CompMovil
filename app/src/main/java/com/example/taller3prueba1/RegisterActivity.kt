package com.example.taller3prueba1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: DatabaseReference
    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            findViewById<ImageView>(R.id.profileImage).setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val selectImageBtn = findViewById<Button>(R.id.selectImageBtn)

        selectImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        registerBtn.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailInput).text.toString().trim()
            val password = findViewById<EditText>(R.id.passwordInput).text.toString().trim()
            val name = findViewById<EditText>(R.id.nameInput).text.toString().trim()
            val lastname = findViewById<EditText>(R.id.lastnameInput).text.toString().trim()
            val id = findViewById<EditText>(R.id.idInput).text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || lastname.isEmpty() || id.isEmpty() || imageUri == null) {
                Toast.makeText(this, "Completa todos los campos y selecciona una imagen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    val ref = storage.reference.child("profiles/$uid.jpg")
                    ref.putFile(imageUri!!)
                        .continueWithTask { ref.downloadUrl }
                        .addOnSuccessListener { uri ->
                            val userMap = mapOf(
                                "nombre" to name,
                                "apellido" to lastname,
                                "id" to id,
                                "email" to email,
                                "imagen" to uri.toString(),
                                "latitud" to 0.0,
                                "longitud" to 0.0,
                                "estado" to "desconectado"
                            )
                            database.child("usuarios").child(uid).setValue(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al guardar datos: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al subir imagen: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al registrar: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}