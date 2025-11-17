package com.example.pocketgarden

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val savedLang = LocaleHelper.loadLocale(newBase)  // get saved language
        val context = LocaleHelper.setLocale(newBase, savedLang)  // wrap context with locale
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        if(savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignUpFragment())
                .commit()
        }
    }
}