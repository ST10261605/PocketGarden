package com.example.pocketgarden

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val getStarted = findViewById<Button?>(R.id.button2)
        getStarted.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
            finish() // optional: closes splash so user can’t go back
        })
    }
}