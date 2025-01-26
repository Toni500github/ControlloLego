package org.toni.controllolego

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import org.toni.controllolego.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetooth()
        }

        // gotta add the same thing on the images too for UX, because that's the important thing
        binding.buttonLeft.setOnTouchListener { view, event -> startAnimation(view, event) }
        binding.buttonLeft.setOnClickListener {
            binding.textToApply.text = "Girando a sinistra di 90°"
            binding.textToApplyBt.text = "P-090"
        }
        binding.buttonLeftImage.setOnTouchListener { _, event -> startAnimation(binding.buttonLeft, event) }
        binding.buttonLeftImage.setOnClickListener {
            binding.textToApply.text = "Girando a sinistra di 90°"
            binding.textToApplyBt.text = "P-090"
        }

        binding.buttonCenter.setOnTouchListener { view, event -> startAnimation(view, event) }
        binding.buttonCenter.setOnClickListener {
            binding.textToApply.text = "Ritornando a 0°"
            binding.textToApplyBt.text = "P000"
        }
        binding.buttonCenterImage.setOnTouchListener { _, event -> startAnimation(binding.buttonCenter, event) }
        binding.buttonCenterImage.setOnClickListener {
            binding.textToApply.text = "Ritornando a 0°"
            binding.textToApplyBt.text = "P000"
        }

        binding.buttonRight.setOnTouchListener { view, event -> startAnimation(view, event) }
        binding.buttonRight.setOnClickListener {
            binding.textToApply.text = "Girando a destra di 90°"
            binding.textToApplyBt.text = "P090"
        }
        binding.buttonRightImage.setOnTouchListener { _, event -> startAnimation(binding.buttonRight, event) }
        binding.buttonRightImage.setOnClickListener {
            binding.textToApply.text = "Girando a destra di 90°"
            binding.textToApplyBt.text = "P090"
        }

        binding.redSlider.addOnChangeListener { _, value, _ ->
            binding.redColor.text = value.toInt().toString()
            setRgbViewColor()
        }
        binding.greenSlider.addOnChangeListener { _, value, _ ->
            binding.greenColor.text = value.toInt().toString()
            setRgbViewColor()
        }
        binding.blueSlider.addOnChangeListener { _, value, _ ->
            binding.blueColor.text = value.toInt().toString()
            setRgbViewColor()
        }

        binding.radioSelectColorMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_rgb_colors -> {
                    binding.customColorSelect.visibility = View.GONE
                    binding.rgbColorSelect.visibility = View.VISIBLE
                }
                R.id.radio_hex_advanced_colors -> {
                    binding.rgbColorSelect.visibility = View.GONE
                    binding.customColorSelect.visibility = View.VISIBLE
                    setColorPickerView()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun setColorPickerView() {
        // disable scroll when interacting with the color picker
        binding.colorPickerView.setOnTouchListener { view, _ ->
            // allow colorPickerView to handle the touch event
            view.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        binding.brightnessSlideBar.setOnTouchListener { view, _ ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.colorPickerHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val col = s.toString()
                if (isValidHex(col))
                    binding.colorPickerView.setInitialColor(col.toColorInt())
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        } )

        binding.colorPickerView.setColorListener(ColorEnvelopeListener { envelope, fromUser ->
            if (!binding.colorPickerHex.text.contentEquals("#"+envelope.hexCode.substring(2)) && fromUser)
                binding.colorPickerHex.setText("#"+envelope.hexCode.substring(2))

            binding.customColorView.setBackgroundColor(envelope.color)
        })

        binding.colorPickerView.attachBrightnessSlider(binding.brightnessSlideBar)
    }

    private fun setRgbViewColor() {
        val red = binding.redColor.text.toString().toInt()
        val green = binding.greenColor.text.toString().toInt()
        val blue = binding.blueColor.text.toString().toInt()
        binding.apply {
            colorView.setBackgroundColor(Color.rgb(red, green, blue))
            hexCode.text = String.format("#%02X%02X%02X", red, green, blue)
        }
    }

    private fun startAnimation(view: View, event: MotionEvent, scaleAnimation: Boolean = false): Boolean {
        if (scaleAnimation) {
            val animRes = when (event.action) {
                MotionEvent.ACTION_DOWN -> R.anim.scale_down
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> R.anim.scale_up
                else -> return false
            }

            val animation = AnimationUtils.loadAnimation(this, animRes)
            view.startAnimation(animation)
            return false
        }

        val drawable = view.background as GradientDrawable
        val colorAnimator = when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                ValueAnimator.ofObject(ArgbEvaluator(),
                    getColor(R.color.buttonBg),
                    getColor(R.color.reverseButtonBg))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                ValueAnimator.ofObject(ArgbEvaluator(),
                    getColor(R.color.reverseButtonBg),
                    getColor(R.color.buttonBg))
            }
            else -> ValueAnimator()
        }
        colorAnimator.duration = 300
        colorAnimator.addUpdateListener { animator ->
            drawable.setColor(animator.animatedValue as Int)
        }
        colorAnimator.start()
        return false
    }

    private fun isValidHex(color: String): Boolean =
        color.matches("^#[0-9A-Fa-f]{6}$".toRegex())

    // https://stackoverflow.com/a/69972855
    fun requestBluetooth() {
        // check android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
        }
    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // granted
            } else {
                // denied
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MyTag", "${it.key} = ${it.value}")
            }
        }
}
