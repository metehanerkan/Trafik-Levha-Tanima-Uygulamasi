package com.example.mobil_uygulama.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobil_uygulama.R

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val editIp = findViewById<EditText>(R.id.editIpAddress)
        val buttonSave = findViewById<Button>(R.id.buttonSaveIp)

        // Kaydedilmiş IP'yi göster
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("server_ip", "")
        editIp.setText(savedIp)

        // Kaydet butonu
        buttonSave.setOnClickListener {
            val ip = editIp.text.toString()
            if (ip.isNotBlank()) {
                prefs.edit().putString("server_ip", ip).apply()
                Toast.makeText(this, "IP kaydedildi: $ip", Toast.LENGTH_SHORT).show()
                finish() // geri dön
            } else {
                Toast.makeText(this, "Lütfen geçerli bir IP girin", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

