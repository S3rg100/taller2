package com.example.taller2

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
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
import android.widget.EditText
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
import java.io.IOException  // <-- Asegúrate de que esta línea esté presente
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var polylineOptions: PolylineOptions
    private lateinit var binding: ActivityMapBinding
    private lateinit var locationSearch: EditText
    private val routePoints: ArrayList<LatLng> = ArrayList()
    private lateinit var currentLocation: LatLng
    private val apiKey = "AIzaSyD7P0JGn3HQtwoVhqwUtu8lytE3gKssBvw"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor != null) {
            val lightSensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (::mMap.isInitialized) {
                        val lightLevel = event.values[0]
                        if (lightLevel < 50) {
                            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapActivity, R.raw.map_night))
                        } else {
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

        locationSearch = findViewById(R.id.location_search)

        // Configurar búsqueda cuando el usuario presiona Enter o finaliza la edición
        locationSearch.setOnEditorActionListener { _, _, _ ->
            val location = locationSearch.text.toString()
            if (location.isNotEmpty()) {
                searchLocationAndRoute(location)  // Llama a la función de búsqueda y creación de ruta
            } else {
                Toast.makeText(this, "Por favor, ingresa una dirección.", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Inicializar PolylineOptions
        polylineOptions = PolylineOptions().width(5f).color(android.graphics.Color.BLUE).geodesic(true)

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

        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_day)
            )
            if (!success) {
                Toast.makeText(this, "Error al aplicar el estilo del mapa.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Resources.NotFoundException) {
            Toast.makeText(this, "No se encontró el archivo de estilo del mapa.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mMap.isMyLocationEnabled = true

        // Agregar Polyline al mapa
        mMap.addPolyline(polylineOptions)

        // LongClickListener para agregar marcadores con la dirección obtenida y dibujar la ruta
        mMap.setOnMapLongClickListener { latLng ->
            val geocoder = Geocoder(this, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0].getAddressLine(0)
                    mMap.addMarker(MarkerOptions().position(latLng).title(address))
                    drawRoute(currentLocation, latLng)  // Crear ruta entre ubicación actual y el punto seleccionado
                } else {
                    Toast.makeText(this, "No se encontró la dirección", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al obtener la dirección", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)

        // Agregar nueva ubicación al array de puntos
        routePoints.add(currentLocation)

        // Añadir el nuevo punto al Polyline
        polylineOptions.add(currentLocation)

        // Limpiar y redibujar el Polyline con los puntos actualizados
        mMap.clear()
        mMap.addPolyline(polylineOptions)

        // Mover la cámara a la nueva ubicación
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
    }

    // Función para buscar una ubicación por dirección y dibujar ruta
    private fun searchLocationAndRoute(location: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(location, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)

                // Añadir marcador en la ubicación encontrada
                mMap.addMarker(MarkerOptions().position(latLng).title(location))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                // Dibujar la ruta entre la ubicación actual y la ubicación encontrada
                drawRoute(currentLocation, latLng)

                Toast.makeText(this, "Ubicación encontrada: ${address.getAddressLine(0)}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se encontró la dirección.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al buscar la dirección.", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para trazar la ruta usando Google Directions API
    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val url = getDirectionUrl(origin, destination)
        GetDirection(url).execute()
    }

    private fun getDirectionUrl(origin: LatLng, destination: LatLng): String {
        val strOrigin = "origin=${origin.latitude},${origin.longitude}"
        val strDest = "destination=${destination.latitude},${destination.longitude}"
        val sensor = "sensor=false"
        val parameters = "$strOrigin&$strDest&$sensor&key=$apiKey"
        return "https://maps.googleapis.com/maps/api/directions/json?$parameters"
    }

    // AsyncTask para realizar la solicitud de la ruta
    private inner class GetDirection(val url: String) : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String {
            var data = ""
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val buffer = StringBuffer()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    buffer.append(line)
                }
                data = buffer.toString()
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            val parserTask = ParserTask()
            parserTask.execute(result)
        }
    }

    // AsyncTask para analizar la respuesta JSON
    private inner class ParserTask : AsyncTask<String, Int, List<List<LatLng>>>() {
        override fun doInBackground(vararg jsonData: String?): List<List<LatLng>> {
            val routes = ArrayList<List<LatLng>>()
            try {
                val jsonObject = JSONObject(jsonData[0])
                val routesArray = jsonObject.getJSONArray("routes")
                for (i in 0 until routesArray.length()) {
                    val path = ArrayList<LatLng>()
                    val legs = routesArray.getJSONObject(i).getJSONArray("legs")
                    for (j in 0 until legs.length()) {
                        val steps = legs.getJSONObject(j).getJSONArray("steps")
                        for (k in 0 until steps.length()) {
                            val polyline = steps.getJSONObject(k).getJSONObject("polyline").getString("points")
                            val points = decodePolyline(polyline)
                            path.addAll(points)
                        }
                    }
                    routes.add(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return routes
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            for (path in result) {
                val polylineOptions = PolylineOptions().addAll(path).width(10f).color(android.graphics.Color.RED)
                mMap.addPolyline(polylineOptions)
            }
        }
    }

    // Decodificar la polyline codificada de Google Directions
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }
        return poly
    }
}
