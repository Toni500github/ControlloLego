package org.toni.controllolego

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
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
import android.view.View.OnTouchListener
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import org.toni.controllolego.databinding.ActivityMainBinding
import java.util.Locale

data class BtStuff(var service: BluetoothService? = null,
                   var writer: BluetoothWriter? = null,
                   var pairedDevices: MutableSet<BluetoothDevice>? = null,
                   var connected: Boolean = false,
                   var bindingMainActivity: ActivityMainBinding? = null)

@SuppressLint("ClickableViewAccessibility", "MissingPermission", "SetTextI18n")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val BTAdapter = BluetoothAdapter.getDefaultAdapter()
    private val btStuff = BtStuff()

    private var rotationAnimator: ObjectAnimator? = null
    private var rotatingButton: ImageButton? = null // Track the rotating button

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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetooth()
        }

        btStuff.pairedDevices = BTAdapter.bondedDevices
        btStuff.bindingMainActivity = binding
        binding.connectHc05.setOnTouchListener { view, event -> startAnimation(view, event) }
        binding.connectHc05.setOnClickListener {
            if (btStuff.connected) {
                btStuff.service?.disconnect()
                btStuff.writer = null
                binding.connectHc05.text = "CONNETTI"
                binding.statusHc05.text = "Dispositivo disconnesso"
                binding.statusHc05.setTextColor(resources.getColor(R.color.subText, null))
                btStuff.connected = false
                return@setOnClickListener
            }
            if (BTAdapter == null) {
                // Device does not support Bluetooth
            } else if (!BTAdapter.isEnabled) {
                val enableBT = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBT, 2)
            } else {
                btStuff.pairedDevices = BTAdapter.bondedDevices
            }

            val btFragment = BtPairedDevices(btStuff)
            setFragment(btFragment)
        }

        setBtButton(binding.buttonLeft, binding.buttonLeftImage,"Girando a sinistra di 90°", "S")
        setBtButton(binding.buttonCenter, binding.buttonCenterImage,"Ritornando a 0°", "C")
        setBtButton(binding.buttonRight, binding.buttonRightImage,"Girando a destra di 90°", "D")

        setSliderBar(binding.redSlider, binding.redColor, "R")
        setSliderBar(binding.greenSlider, binding.greenColor, "G")
        setSliderBar(binding.blueSlider, binding.blueColor, "B")

        val greenColorStateList = resources.getColorStateList(R.color.green, null)
        val redColorStateList = resources.getColorStateList(R.color.red, null)
        var isClickedLeft = false
        var isClickedRight = false

        binding.buttonLeft2.setOnClickListener { button ->
            if (btStuff.writer != null) {
                showHCNotConnected()
                return@setOnClickListener
            }
            isClickedLeft = !isClickedLeft
            isClickedRight = false
            if (isClickedLeft) {
                binding.buttonRight2.backgroundTintList = redColorStateList
                button.backgroundTintList = greenColorStateList
                binding.textToApply.text = "Ruotando a sinistra"
                binding.textToApplyBt.text = "I"
                runOnUiThread { startRotationAnimation(binding.buttonLeft2, true) }
            } else {
                button.backgroundTintList = redColorStateList
                binding.textToApply.text = "Fermo"
                binding.textToApplyBt.text = "F"
                stopRotationAnimation()
            }
            btStuff.writer?.write(binding.textToApplyBt.text.toString())
        }

        binding.buttonRight2.setOnClickListener { button ->
            if (btStuff.writer != null) {
                showHCNotConnected()
                return@setOnClickListener
            }
            isClickedRight = !isClickedRight
            isClickedLeft = false
            if (isClickedRight) {
                binding.buttonLeft2.backgroundTintList = redColorStateList
                button.backgroundTintList = greenColorStateList
                binding.textToApply.text = "Ruotando a destra"
                binding.textToApplyBt.text = "A"
                runOnUiThread { startRotationAnimation(binding.buttonRight2, false) }
            } else {
                button.backgroundTintList = redColorStateList
                binding.textToApply.text = "Fermo"
                binding.textToApplyBt.text = "F"
                stopRotationAnimation()
            }
            btStuff.writer?.write(binding.textToApplyBt.text.toString())
        }

        binding.textToBluetooth.setOnTouchListener { view, _ ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.sendText.setOnClickListener {
            if (binding.textToBluetooth.text.isNotEmpty())
                btStuff.writer?.write(binding.textToBluetooth.text.toString()) ?: showHCNotConnected()
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

    private fun startRotationAnimation(button: ImageButton, isLeft: Boolean) {
        stopRotationAnimation() // Stop any previous rotation
        rotatingButton = button
        rotationAnimator = ObjectAnimator.ofFloat(
            button,
            "rotation",
            if (isLeft) 0f else 360f,
            if (isLeft) -360f else 0f
        ).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopRotationAnimation() {
        rotationAnimator?.cancel()
        rotatingButton?.rotation = 0f // Reset rotation to avoid weird angles
        rotatingButton = null
    }

    private fun showHCNotConnected() {
        Toast.makeText(this, "Connettersi al dispositivo HC-05 prima", Toast.LENGTH_SHORT).show()
    }

    private fun setSliderBar(slider: Slider, textView: TextView, color: String) {
        slider.addOnChangeListener { _, value, _ ->
            val str = String.format(Locale.ENGLISH, "%03d", value.toInt())
            textView.text = str
            setRgbViewColor()

            if (btStuff.writer == null)
                return@addOnChangeListener
            btStuff.writer?.write(color)
            btStuff.writer?.write(str)
        }
    }

    private fun setFragment(fragment: Fragment, slideInAnim: Int = R.anim.slide_in) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                slideInAnim,  // enter
                android.R.animator.fade_out,  // exit
                android.R.animator.fade_in,   // popEnter
                R.anim.slide_out  // popExit
            )
            .replace(android.R.id.content, fragment)
            .addToBackStack(null).commit()
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

    private fun setBtButton(button: View, imageView: ImageView, textToApply: String, textToApplyBt: String) {
        button.setOnTouchListener { view, event -> startAnimation(view, event); startAnimation(imageView, event) }
        button.setOnClickListener {
            binding.textToApply.text =  textToApply
            binding.textToApplyBt.text = textToApplyBt
            btStuff.writer?.write(textToApplyBt) ?: showHCNotConnected()
        }
        imageView.setOnTouchListener { _, event -> startAnimation(button, event); startAnimation(imageView, event) }
        imageView.setOnClickListener {
            binding.textToApply.text =  textToApply
            binding.textToApplyBt.text = textToApplyBt
            btStuff.writer?.write(textToApplyBt) ?: showHCNotConnected()
        }
    }

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

            if (btStuff.writer != null) {
                btStuff.writer?.write("R" + String.format(Locale.ENGLISH, "%03d", envelope.color.red))
                btStuff.writer?.write("G" + String.format(Locale.ENGLISH, "%03d", envelope.color.green))
                btStuff.writer?.write("B" + String.format(Locale.ENGLISH, "%03d", envelope.color.blue))
            }
        })

        binding.colorPickerView.attachBrightnessSlider(binding.brightnessSlideBar)
    }

    private fun isValidHex(color: String): Boolean =
        color.matches("^#[0-9A-Fa-f]{6}$".toRegex())

    private fun startAnimation(view: View, event: MotionEvent, scaleAnimation: Boolean = false): Boolean =
        startAnimation(this, view, event, scaleAnimation)

    private fun startAnimation(imageView: ImageView, event: MotionEvent): Boolean {
        // Return early for unhandled motion events
        if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_CANCEL)
            return false

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
            else -> return false
        }
        colorAnimator.duration = 300
        colorAnimator.addUpdateListener { animator ->
            imageView.background.setTint(animator.animatedValue as Int)
        }
        colorAnimator.start()
        return false
    }

    // https://stackoverflow.com/a/69972855
    private fun requestBluetooth() {
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

    override fun onDestroy() {
        super.onDestroy()
        stopRotationAnimation()
    }
}

internal fun startAnimation(context: Context, view: View, event: MotionEvent, scaleAnimation: Boolean = false): Boolean {
    if (scaleAnimation) {
        val animRes = when (event.action) {
            MotionEvent.ACTION_DOWN -> R.anim.scale_down
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> R.anim.scale_up
            else -> return false
        }

        val animation = AnimationUtils.loadAnimation(context, animRes)
        view.startAnimation(animation)
        return false
    }
    // Return early for unhandled motion events
    if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_CANCEL)
        return false

    val drawable = view.background as GradientDrawable
    val colorAnimator = when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            ValueAnimator.ofObject(ArgbEvaluator(),
                ContextCompat.getColor(context, R.color.buttonBg),
                ContextCompat.getColor(context, R.color.reverseButtonBg))
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            ValueAnimator.ofObject(ArgbEvaluator(),
                ContextCompat.getColor(context, R.color.reverseButtonBg),
                ContextCompat.getColor(context, R.color.buttonBg))
        }
        else -> return false
    }
    colorAnimator.duration = 300
    colorAnimator.addUpdateListener { animator ->
        drawable.setColor(animator.animatedValue as Int)
    }
    colorAnimator.start()
    return false
}
