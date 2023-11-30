package com.example.trabalho_sd

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    lateinit var filho: Button
    lateinit var pai: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        filho=findViewById(R.id.filho)
        pai=findViewById(R.id.pai)
        pai.setOnClickListener{

            try {
                telaPai()
            }catch (e:Exception){
                e.printStackTrace()
            }


        }
        filho.setOnClickListener{

            telaFilho()
        }
    }

    private fun telaFilho(){
        val telaFilho = Intent(this,tela_filho::class.java)
        startActivity(telaFilho)
    }
    private fun telaPai(){
        val telaPai = Intent(this,tela_pai::class.java)
        startActivity(telaPai)
    }
}