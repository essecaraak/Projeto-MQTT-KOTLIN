package com.example.trabalho_sd

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
private lateinit var lat: TextView
private lateinit var long: TextView
private lateinit var botao: Button

class tela_filho : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_filho)
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this)
        lat = findViewById(R.id.altitude)
        long = findViewById(R.id.longitude)
        botao = findViewById(R.id.botao)
        botao.setOnClickListener{

            encontrarCoordenadas()
        }


    }


    private fun encontrarCoordenadas(){
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)
            !=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),1)
        }else{
            getLocations()
        }

    }

    @SuppressLint("MissingPermission")
    private fun getLocations() {
        fusedLocationProviderClient.lastLocation?.addOnSuccessListener {
            if (it==null){
                Toast.makeText(this,"não deu pra pegar a localização",Toast.LENGTH_LONG).show()
            }else{
                it.apply {
                    val latitude= it.latitude
                    val longitude= it.longitude
                    lat.text="latitude: $latitude"
                    long.text= "longitude: $longitude"

                }
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==1){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"Permissão dada",Toast.LENGTH_LONG).show()
                    getLocations()
                }else{
                    Toast.makeText(this,"Permissão negada",Toast.LENGTH_LONG).show()

                }
            }
        }
    }
}