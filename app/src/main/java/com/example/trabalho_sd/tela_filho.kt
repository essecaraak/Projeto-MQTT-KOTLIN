package com.example.trabalho_sd

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
private lateinit var lat: TextView
private lateinit var long: TextView
private lateinit var cidade: TextView
private lateinit var botao: Button
var PERMISSION_ID=1010

class tela_filho : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_filho)
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this)
        lat = findViewById(R.id.altitude)
        long = findViewById(R.id.longitude)
        cidade = findViewById(R.id.cidade)
        botao = findViewById(R.id.botao)
        botao.setOnClickListener{

            Log.d("Debug:",checkPermission().toString())
            Log.d("Debug:",isLocationEnabled().toString())
            RequestPermission()
            getLastLocation()

        }


    }


    private fun checkPermission():Boolean{
        return !(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
    }
    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        if(checkPermission()){
            if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener {task->
                    var location: Location? = task.result
                    if(location == null){
                        Toast.makeText(this,"Localização nula",Toast.LENGTH_SHORT).show()
                    }else{
                        Log.d("Debug:" ,"Your Location:"+ location.longitude)
                        lat.text = "latitude: "+ location.latitude
                        long.text = "longitude: "+ location.longitude
                        cidade.text = "cidade: "+ getCityName(location.latitude,location.longitude)
                    }
                }
            }else{
                Toast.makeText(this,"Liga a localização",Toast.LENGTH_SHORT).show()
            }
        }else{
            RequestPermission()
        }
    }

    /*private fun getCityName(lat: Double,long: Double):String{
        var cityName:String = ""
        var countryName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var Adress = geoCoder.getFromLocation(lat,long,3)

        if (Adress != null) {
            cityName = Adress.get(0).locality
        }
        if (Adress != null) {
            countryName = Adress.get(0).countryName
        }
        Log.d("Debug:","Your City: " + cityName + " ; your Country " + countryName)
        return cityName
    } */

    private fun getCityName(lat: Double, long: Double): String {
        var cityName: String = ""
        var countryName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var Adress = geoCoder.getFromLocation(lat, long, 3)

        if (Adress != null) {
            if (Adress.isNotEmpty()) {
                Adress?.get(0)?.let { address ->
                    cityName = address.locality ?: ""
                    countryName = address.countryName ?: ""
                }
            }
        }

        Log.d("Debug:", "Your City: $cityName; Your Country: $countryName")
        return cityName
    }


    private fun RequestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }
    private fun isLocationEnabled():Boolean{

        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_ID){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d("Debug:","deu certo")
            }
        }
    }
}