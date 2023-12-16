package com.example.trabalho_sd

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale
import kotlin.math.log

private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
private lateinit var lat: TextView
private lateinit var long: TextView
public lateinit var topico: EditText
private lateinit var exibirTopico: TextView
private lateinit var botaoConecta: Button
private lateinit var botaoDesconecta: Button
private lateinit var botaoMapaFilho: Button
private  var flagconect=0
var PERMISSION_ID=1010
private lateinit var mqttClient: MqttAndroidClient
private  var TAG="mqtt"
private  lateinit var valtopico: String

class tela_filho : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_filho)
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this)
        lat = findViewById(R.id.altitude)
        long = findViewById(R.id.longitude)
        topico = findViewById(R.id.textoTopico)
        exibirTopico = findViewById(R.id.textoExibirTopico)
        botaoConecta = findViewById(R.id.botaoConecta)
        botaoDesconecta = findViewById(R.id.botaoDesconecta)
        botaoMapaFilho = findViewById(R.id.botaoMapaFilho)
        botaoConecta.setOnClickListener{
            if(flagconect==1){
                Toast.makeText( this,"Desconecte antes de conectar outro tópico",Toast.LENGTH_SHORT).show()
            }else if(topico.text.isEmpty()){
                Toast.makeText(this,"Insira um tópico antes",Toast.LENGTH_SHORT).show()
            }else{
                connect(this)
                EventBus.getDefault().register(this)
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    startService(this)

                }

            }
        }
        botaoDesconecta.setOnClickListener{
            if(flagconect==1){
                disconnect(this)
                EventBus.getDefault().unregister(this)
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
                    startService(this)
                }
            }else{
                Toast.makeText(this,"nenhum tópico conectado",Toast.LENGTH_SHORT).show()

            }

        }

        botaoMapaFilho.setOnClickListener{
            if(flagconect==1){
                Toast.makeText( this,"Desconecte antes de conectar outro tópico",Toast.LENGTH_SHORT).show()
            }else if(topico.text.isEmpty()){
                Toast.makeText(this,"Insira um tópico antes",Toast.LENGTH_SHORT).show()
            }else{
                connect(this)
                EventBus.getDefault().register(this)
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    startService(this)


                }

            }
        }



    }
    //pega as coisas do serviço e publica, não mexer
@Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
fun onEvent(result:ResultData){
    lat.text = "Latitude: "+ result.lat
    long.text = "Longitude: "+ result.long
    Log.d(TAG,""+topico.text)
    publish(valtopico,""+result.lat+","+result.long)
}
    fun connect(context: Context) { //não mudie nada e funcinou kkkkk
        val serverURI = "ssl://e59f8ed61b8e47abb5e1752437996eda.s2.eu.hivemq.cloud:8883"
        var recCount = 0
        mqttClient = MqttAndroidClient(context, serverURI, "kotlin_client")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                recCount = recCount + 1
                Log.d(TAG, "Received message ${recCount}: ${message.toString()} from topic: $topic")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")

            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
        options.userName = "TrabalhoSD"
        options.password = "Maltar123".toCharArray()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    flagconect=1
                    subscribe(""+topico.text)
                    exibirTopico.text="Tópico: "+ topico.text
                    valtopico=""+topico.text
                    topico.setText("")
                    Toast.makeText(context,"conexão feita com sucesso",Toast.LENGTH_SHORT).show()
                    RequestPermission()


                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure")
                    Toast.makeText(context,"falha na conexão",Toast.LENGTH_SHORT).show()

                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }
    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            mqttClient.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Unsubscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to unsubscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /*fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    } */

    fun publish(topic: String, msg: String, qos: Int = 2, retained: Boolean = false) {
        if (::mqttClient.isInitialized && mqttClient.isConnected) { // Verifica se mqttClient está inicializado e conectado
            try {
                // Se estiver inicializado e conectado, então publica a mensagem
                val message = MqttMessage()
                message.payload = msg.toByteArray()
                message.qos = qos
                message.isRetained = retained
                mqttClient.publish(topic, message, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "$msg published to $topic")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(TAG, "Failed to publish $msg to $topic")
                    }
                })
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        } else {
            Log.d(TAG, "mqttClient is not initialized or not connected")
        }
    }

    fun disconnect(context: Context) {
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                    flagconect=0
                    exibirTopico.text="Tópico: "
                    lat.text="Latitude: "
                    long.text="Longitude: "

                    Toast.makeText(context,"Desconectado com sucessso",Toast.LENGTH_SHORT).show()

                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                    Toast.makeText(context,"Falha ao desconectar",Toast.LENGTH_SHORT).show()


                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
    private fun checkPermission():Boolean{
        return !(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
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
                        lat.text = "Latitude: "+ location.latitude
                        long.text = "Longitude: "+ location.longitude
                        publish(""+topico.text,""+location.latitude+","+location.longitude)
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
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }
    private fun isLocationEnabled():Boolean{

        var locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
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