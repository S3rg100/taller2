package com.example.taller2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class CameraActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var switchVideo: SwitchMaterial
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_VIDEO_CAPTURE = 3
    private val REQUEST_PICK_IMAGE = 2
    private val REQUEST_PICK_VIDEO = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        imageView = findViewById(R.id.image_view)
        videoView = findViewById(R.id.video_view)
        switchVideo = findViewById(R.id.switch_video)

        val btnOpenCamera: Button = findViewById(R.id.btn_open_camera)
        val btnOpenGallery: Button = findViewById(R.id.btn_open_gallery)

        // Registrar el lanzador de permisos de cámara
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Si el permiso fue otorgado
                if (switchVideo.isChecked) {
                    // Iniciar la grabación de video
                    dispatchTakeVideoIntent()
                } else {
                    // Iniciar la captura de imagen
                    dispatchTakePictureIntent()
                }
            } else {
                // Si el permiso fue denegado
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

        // Acción para abrir la cámara
        btnOpenCamera.setOnClickListener {
            checkCameraPermission()
        }

        // Acción para abrir la galería
        btnOpenGallery.setOnClickListener {
            if (switchVideo.isChecked) {
                // Seleccionar un video de la galería
                val pickVideoIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickVideoIntent, REQUEST_PICK_VIDEO)
            } else {
                // Seleccionar una foto de la galería
                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE)
            }
        }
    }

    // Solicitar permisos de cámara si es necesario
    private fun checkCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Explicación del permiso de cámara
                Toast.makeText(this, "Se necesita acceso a la cámara para tomar fotos y videos", Toast.LENGTH_LONG).show()
            }
            else -> {
                // Lanzar la solicitud de permiso de cámara
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Iniciar la actividad de captura de foto
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    // Iniciar la actividad de captura de video
    private fun dispatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (takeVideoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }

    // Manejar el resultado de la cámara o la galería
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val extras: Bundle? = data?.extras
                    val imageBitmap: Bitmap? = extras?.get("data") as? Bitmap
                    imageView.setImageBitmap(imageBitmap)
                    imageView.visibility = ImageView.VISIBLE
                    videoView.visibility = VideoView.GONE
                }

                REQUEST_VIDEO_CAPTURE -> {
                    val videoUri: Uri? = data?.data
                    videoView.setVideoURI(videoUri)
                    videoView.start()
                    videoView.visibility = VideoView.VISIBLE
                    imageView.visibility = ImageView.GONE
                }

                REQUEST_PICK_IMAGE -> {
                    val selectedImage: Uri? = data?.data
                    imageView.setImageURI(selectedImage)
                    imageView.visibility = ImageView.VISIBLE
                    videoView.visibility = VideoView.GONE
                }

                REQUEST_PICK_VIDEO -> {
                    val selectedVideo: Uri? = data?.data
                    videoView.setVideoURI(selectedVideo)
                    videoView.start()
                    videoView.visibility = VideoView.VISIBLE
                    imageView.visibility = ImageView.GONE
                }
            }
        }
    }
}