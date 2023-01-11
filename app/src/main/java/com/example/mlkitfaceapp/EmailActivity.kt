package com.example.mlkitfaceapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.example.mlkitfaceapp.databinding.ActivityEmailBinding
import com.google.android.material.button.MaterialButton

class EmailActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityEmailBinding>(this, R.layout.activity_email)
        //setContentView(R.layout.activity_email)

        val emailView = findViewById<EditText>(R.id.etEmail)


        binding.submitBtn.setOnClickListener {
            if (emailView.text.toString().isNullOrEmpty().not()) {
                startActivity(Intent(this@EmailActivity, MainActivity::class.java)
                    .putExtra("email", emailView.text.toString()))
            }
        }
    }
}

@SuppressLint("NewApi")
@BindingAdapter("size")
fun setTextFromResourceWithTemplate(
    textView: TextView,
    @StyleRes size: Int
) {
   textView.setTextAppearance(size)
}