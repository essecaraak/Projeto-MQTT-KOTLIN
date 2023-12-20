package com.example.trabalho_sd

import android.Manifest
import kotlin.math.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import java.text.DecimalFormat
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
import info.mqtt.android.service.MqttAndroidClient
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
import java.lang.Math.sin
import java.util.Locale

private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
private lateinit var friend_coords: TextView
private lateinit var self_coords: TextView
public lateinit var topico: EditText
public lateinit var topicoAmigo: EditText
private lateinit var exibirTopico: TextView
private lateinit var exibirTopicoAmigo: TextView
private lateinit var exibirDistancia: TextView
private lateinit var botaoConecta: Button
private lateinit var botaoDesconecta: Button
private lateinit var botaoMapaFilho: Button
var loc1: Double = -1.0
var loc2: Double = -1.0
var loc1Amigo: Double = -1.0
var loc2Amigo: Double = -1.0
private  var flagconect=0
var PERMISSION_ID=1010
private lateinit var mqttClient: MqttAndroidClient
private  var TAG="mqtt"
private  lateinit var valtopico_self: String
private  lateinit var valtopico_amigo: String

class tela_filho : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_filho)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        friend_coords = findViewById(R.id.Friend_coords)
        self_coords = findViewById(R.id.Self_coords)
        topico = findViewById(R.id.textoTopico)
        topicoAmigo = findViewById(R.id.textoTopico_amigo)
        exibirTopico = findViewById(R.id.textoExibirTopico_self)
        exibirTopicoAmigo = findViewById(R.id.textoExibirTopico_amigo)
        exibirDistancia = findViewById(R.id.ExibirDistancia)
        botaoConecta = findViewById(R.id.botaoConecta)
        botaoDesconecta = findViewById(R.id.botaoDesconecta)
        botaoMapaFilho = findViewById(R.id.botaoMapaFilho)

        checkPermission()
        botaoConecta.setOnClickListener {
            if (flagconect == 1) {
                Toast.makeText(
                    this,
                    "Desconecte antes de se conectar novamente",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (topico.text.isEmpty() || topicoAmigo.text.isEmpty()) {
                Toast.makeText(this, "Insira os dois CPFs ", Toast.LENGTH_SHORT).show()
            } else {
                checkPermission()
                connect(this)
                EventBus.getDefault().register(this)
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    startService(this)

                }

            }
        }
        botaoDesconecta.setOnClickListener {
            if (flagconect == 1) {
                unsubscribe(""+topicoAmigo)
                disconnect(this)
                EventBus.getDefault().unregister(this)
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
                    startService(this)
                }
            } else {
                Toast.makeText(this, "Já desconectado", Toast.LENGTH_SHORT).show()

            }

        }

        botaoMapaFilho.setOnClickListener {
            if (flagconect == 1) {
                if(loc1Amigo!=-1.0 && loc2Amigo!=-1.0 && loc1!=-1.0 && loc2!=-1.0){
                    telaMapa()
                }else{
                    Toast.makeText(this, "Espere as localizações carregarem", Toast.LENGTH_SHORT).show()

                }
            } else {
                Toast.makeText(this, "nenhum CPF inserido", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun telaMapa(){
        val telaMapa = Intent(this,MapsActivity::class.java)
        startActivity(telaMapa)
    }

    //pega as coisas do serviço e publica, não mexer
@Subscribe(threadMode = ThreadMode.MAIN_ORDERED)

    fun onEvent(result: ResultData) {
        runOnUiThread {

            self_coords.text = "Suas coordenadas:\n(" + result.lat+"," + result.long+")"
            loc1 = result.lat.toDouble()
            loc2 = result.long.toDouble()
            Log.d(TAG, "" + topico.text)
            exibirTopico.text = "Seu CPF: " + valtopico_self
            if(loc1Amigo!=-1.0 && loc2Amigo!=-1.0){
                exibirTopicoAmigo.text = "CPF do seu amigo: " + valtopico_amigo
                friend_coords.text = "Coordenadas do seu amigo:\n("+ loc1Amigo+","+ loc2Amigo+")"
            }

            publish(valtopico_self, "" + result.lat + "," + result.long,2)
            if(loc1Amigo!=-1.0 && loc2Amigo!=-1.0){
                val txt2=calculateDistance(loc1,loc2, loc1Amigo, loc2Amigo)
                exibirDistancia.text="Seu amigo está a "+txt2+" quilômetros de distância"
            }
        }
    }


    fun connect(context: Context) { //não mudie nada e funcinou kkkkk
        val serverURI = "ssl://e59f8ed61b8e47abb5e1752437996eda.s2.eu.hivemq.cloud:8883"
        var recCount = 0
        mqttClient = MqttAndroidClient(context, serverURI, "kotlin_client")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                recCount = recCount + 1
                val txt=""+message
                if(topic == ""+valtopico_amigo){
                    friend_coords.text = "Coordenadas do seu amigo:\n("+message+")"
                    exibirTopicoAmigo.text = "CPF do seu amigo: " + valtopico_amigo
                    exibirTopico.text = "Seu CPF: " + valtopico_self
                    self_coords.text = "Suas coordenadas:\n(" + loc1+"," + loc2+")"
                    val values = txt.split(",")
                    loc1Amigo=values[0].toDouble()
                    loc2Amigo=values[1].toDouble()
                    if(loc1!=-1.0 && loc2!=-1.0){
                        val txt2=calculateDistance(loc1,loc2, loc1Amigo, loc2Amigo)
                        exibirDistancia.text="Seu amigo está a "+txt2+" quilômetros de distância"
                    }

                }
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
                    runOnUiThread {
                        Log.d(TAG, "Connection success")
                        flagconect = 1
                        subscribe("" + topico.text,2)
                        exibirTopico.text = "Seu CPF: " + topico.text
                        valtopico_self = "" + topico.text
                        topico.setText("")
                        subscribe(""+topicoAmigo.text,2)
                        exibirTopicoAmigo.text="CPF do seu amigo: "+topicoAmigo.text
                        valtopico_amigo = "" + topicoAmigo.text
                        topicoAmigo.setText("")
                        friend_coords.text = "Coordenadas do seu amigo:\nAguardando chegada"
                        Toast.makeText(context, "conexão feita com sucesso", Toast.LENGTH_SHORT)
                            .show()
                        RequestPermission()
                    }

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
                    exibirTopico.text="Seu CPF:"
                    self_coords.text = "Suas coordenadas:"
                    exibirTopicoAmigo.text="CPF do seu amigo:"
                    friend_coords.text = "Coordenadas do seu amigo:"

                    runOnUiThread {
                        Toast.makeText(context,"Desconectado com sucessso",Toast.LENGTH_SHORT).show()
                    }



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
    /*private fun getLastLocation(){
        if(checkPermission()){
            if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener {task->
                    var location: Location? = task.result
                    if(location == null){
                        Toast.makeText(this,"Localização nula",Toast.LENGTH_SHORT).show()
                    }else{
                        Log.d("Debug:" ,"Your Location:"+ location.longitude)

                        publish(""+topico.text,""+location.latitude+","+location.longitude)
                    }
                }
            }else{
                Toast.makeText(this,"Liga a localização",Toast.LENGTH_SHORT).show()
            }
        }else{
            RequestPermission()
        }
    }*/

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
    data class Location(val latitude: Double, val longitude: Double)

    fun haversineDistance(point1: Location, point2: Location): Double {
        val R = 6371.0 // raio médio da Terra em quilômetros

        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val point1 = Location(lat1, lon1)
        val point2 = Location(lat2, lon2)
        val distance = haversineDistance(point1, point2)

        // Formata o resultado com duas casas decimais
        val decimalFormat = DecimalFormat("#.##")
        return decimalFormat.format(distance)
    }
}