package com.example.taller2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial

class CameraActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var switchVideo: SwitchMaterial
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>

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
                handleCameraAction()
            } else {
                // Si el permiso fue denegado
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

        // Registrar el lanzador de permisos de galería
        galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Si el permiso fue otorgado
                handleGalleryAction()
            } else {
                // Si el permiso fue denegado
                Toast.makeText(this, "Permiso para acceder a la galería denegado", Toast.LENGTH_SHORT).show()
            }
        }

        // Acción para abrir la cámara
        btnOpenCamera.setOnClickListener {
            checkCameraPermission()
        }

        // Acción para abrir la galería
        btnOpenGallery.setOnClickListener {
            checkGalleryPermission()
        }
    }

    // Permisos de Cámara
    private fun checkCameraPermission() {
        requestPermissionWithSnackbar(
            this,
            Manifest.permission.CAMERA,
            "Se necesita acceso a la cámara para tomar fotos y videos",
            cameraPermissionLauncher,
            ::handleCameraAction // Lanza la acción de cámara si ya tiene los permisos
        )
    }

    // Permisos de Galería
    private fun checkGalleryPermission() {
        when {
            // Para Android 13 (API 33) o superior
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                val permission = if (switchVideo.isChecked) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_MEDIA_IMAGES
                }
                requestPermissionWithSnackbar(
                    this,
                    permission,
                    "Se necesita acceso a la galería para seleccionar archivos desde la galería",
                    galleryPermissionLauncher,
                    ::handleGalleryAction
                )
            }

            // Para Android 11 (API 30) o superior
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R -> {
                requestPermissionWithSnackbar(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    "Se necesita acceso a la galería para seleccionar fotos y videos",
                    galleryPermissionLauncher,
                    ::handleGalleryAction
                )
            }

            // Para versiones anteriores
            else -> {
                requestPermissionWithSnackbar(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    "Se necesita acceso a la galería para seleccionar fotos y videos",
                    galleryPermissionLauncher,
                    ::handleGalleryAction
                )
            }
        }
    }

    // Maneja la acción de la cámara dependiendo de si está seleccionada la opción de video o foto
    private fun handleCameraAction() {
        if (switchVideo.isChecked) {
            dispatchTakeVideoIntent()
        } else {
            dispatchTakePictureIntent()
        }
    }

    // Maneja la acción de la galería dependiendo de si está seleccionada la opción de video o foto
    private fun handleGalleryAction() {
        if (switchVideo.isChecked) {
            val pickVideoIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickVideoIntent, REQUEST_PICK_VIDEO)
        } else {
            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE)
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

    // Lógica para pedir permisos
    private fun requestPermissionWithSnackbar(
        context: Context,
        permission: String,
        rationale: String,
        getSimplePermission: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                Snackbar.make(findViewById(android.R.id.content), "Ya tengo los permisos", Snackbar.LENGTH_LONG).show()
                onPermissionGranted()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                val snackbar = Snackbar.make(findViewById(android.R.id.content), rationale, Snackbar.LENGTH_LONG)
                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        if (event == DISMISS_EVENT_TIMEOUT) {
                            getSimplePermission.launch(permission)
                        }
                    }
                })
                snackbar.show()
            }

            else -> {
                getSimplePermission.launch(permission)
            }
        }
    }
}
