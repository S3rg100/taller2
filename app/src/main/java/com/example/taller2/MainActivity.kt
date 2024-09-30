package com.example.taller2
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Referencias a los botones
        val cameraButton: Button = findViewById(R.id.btn_camera)
        val mapButton: Button = findViewById(R.id.btn_map)

        // Acción al presionar el botón de Cámara
        cameraButton.setOnClickListener {
            // Inicia la actividad CameraActivity
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // Acción al presionar el botón de Mapa
        mapButton.setOnClickListener {
            // Inicia la actividad MapActivity (aún por definir)
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }

}