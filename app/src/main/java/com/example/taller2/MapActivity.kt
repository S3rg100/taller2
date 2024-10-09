package com.example.taller2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller2.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var polylineOptions: PolylineOptions
    private lateinit var binding: ActivityMapBinding
    private val routePoints: ArrayList<LatLng> = ArrayList()

    // Coloca aquí tu clave API de Geocoding
    private val apiKey = "YOUR_GOOGLE_API_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el fragmento del mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializar el sensor de luz
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Verificar si el sensor de luz está disponible
        if (lightSensor != null) {
            val lightSensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // Verificar si mMap ha sido inicializado antes de usarlo
                    if (::mMap.isInitialized) {
                        val lightLevel = event.values[0]
                        if (lightLevel < 50) {
                            // Modo oscuro
                            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapActivity, R.raw.map_night))
                        } else {
                            // Modo claro
                            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapActivity, R.raw.map_day))
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Toast.makeText(this, "Sensor de luz no disponible", Toast.LENGTH_SHORT).show()
        }

        // Manejar búsqueda de direcciones
        binding.locationSearch.setOnEditorActionListener { _, _, _ ->
            val location = binding.locationSearch.text.toString()
            if (location.isNotEmpty()) {
                GeocodingTask().execute(location)  // Usar la API de Geocoding
            }
            true
        }

        polylineOptions = PolylineOptions().width(5f).color(android.graphics.Color.BLUE).geodesic(true)

        // Inicializar el Location Manager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10f, this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Habilitar localización en el mapa
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mMap.isMyLocationEnabled = true

        // Configurar Polyline para dibujar la ruta del usuario
        mMap.addPolyline(polylineOptions)

        // LongClickListener para agregar marcadores en el mapa al hacer click largo
        mMap.setOnMapLongClickListener { latLng ->
            // Obtener dirección con Geocoder
            val geocoder = Geocoder(this, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0)

                    // Añadir marcador con la dirección obtenida
                    mMap.addMarker(MarkerOptions().position(latLng).title(addressText))
                } else {
                    Toast.makeText(this, "No se encontró la dirección", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al obtener la dirección", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        routePoints.add(currentLatLng)

        // Log para verificar si se añaden puntos correctamente
        Log.d("MapActivity", "Ubicación cambiada: ${location.latitude}, ${location.longitude}")

        // Actualizar Polyline con los puntos de la ruta
        polylineOptions.add(currentLatLng)

        // Limpiar el mapa y volver a agregar el Polyline actualizado
        mMap.clear()
        mMap.addPolyline(polylineOptions)

        // Mover la cámara a la nueva ubicación
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
    }

    // Clase asíncrona para hacer la solicitud de Geocoding
    private inner class GeocodingTask : AsyncTask<String, Void, LatLng?>() {
        override fun doInBackground(vararg params: String): LatLng? {
            val locationName = params[0]
            val urlString = "https://maps.googleapis.com/maps/api/geocode/json?address=${locationName.replace(" ", "+")}&key=$apiKey"
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var inputLine: String?
                while (inputStream.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                inputStream.close()

                val jsonResponse = JSONObject(response.toString())
                val results = jsonResponse.getJSONArray("results")
                if (results.length() > 0) {
                    val location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                    val lat = location.getDouble("lat")
                    val lng = location.getDouble("lng")
                    return LatLng(lat, lng)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(latLng: LatLng?) {
            if (latLng != null) {
                mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación encontrada"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this@MapActivity, "No se pudo encontrar la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Clase asíncrona para hacer la solicitud de Reverse Geocoding (con coordenadas)
    private inner class ReverseGeocodingTask : AsyncTask<LatLng, Void, String?>() {
        override fun doInBackground(vararg params: LatLng): String? {
            val latLng = params[0]
            val urlString = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latLng.latitude},${latLng.longitude}&key=$apiKey"
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var inputLine: String?
                while (inputStream.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                inputStream.close()

                val jsonResponse = JSONObject(response.toString())
                val results = jsonResponse.getJSONArray("results")
                if (results.length() > 0) {
                    return results.getJSONObject(0).getString("formatted_address")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(address: String?) {
            if (address != null) {
                mMap.addMarker(MarkerOptions().position(routePoints.last()).title(address))
            } else {
                Toast.makeText(this@MapActivity, "No se pudo encontrar la dirección", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
