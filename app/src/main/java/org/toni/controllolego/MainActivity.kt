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
import android.widget.LinearLayout
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import org.toni.controllolego.databinding.ActivityMainBinding


@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mService: BluetoothService
    private var writer: BluetoothWriter? = null
    private val BTAdapter = BluetoothAdapter.getDefaultAdapter()

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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetooth()
        }

        var alreadyConnected = false
        binding.connectHc05.setOnTouchListener { view, event -> startAnimation(view, event) }
        binding.connectHc05.setOnClickListener {
            if (alreadyConnected) {
                mService.disconnect()
                writer = null
                binding.connectHc05.text = "CONNETTI"
                binding.statusHc05.text = "Dispositivo disconnesso"
                alreadyConnected = false
                return@setOnClickListener
            }
            if (BTAdapter == null) {
                // Device does not support Bluetooth
            } else if (!BTAdapter.isEnabled) {
                val enableBT = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBT, 2)
            }
            val pairedDevices = BTAdapter.bondedDevices
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    Log.d("BluetoothTestingLmao", "device name = ${device?.name}")
                    Log.d("BluetoothTestingLmao", "MAC address = ${device?.address}")
                    if (device?.name.contentEquals("HC-05", true)) {
                        binding.statusHc05.text = "Connettendomi al ${device?.name}..."
                        val uuids = device.uuids
                        if (uuids != null) {
                            val config = BluetoothConfiguration()
                            config.bluetoothServiceClass = BluetoothClassicService::class.java //  BluetoothClassicService.class or BluetoothLeService.class
                            config.context = applicationContext
                            config.bufferSize = 2048
                            config.characterDelimiter = '\n'
                            config.deviceName = "Controllo Lego"
                            config.callListenersInMainThread = true
                            config.uuid = uuids[1].uuid
                            BluetoothService.init(config)
                            mService = BluetoothService.getDefaultInstance()
                            mService.connect(device)
                            while (mService.status == BluetoothStatus.CONNECTING) {  }
                            if (mService.status == BluetoothStatus.CONNECTED) {
                                writer = BluetoothWriter(mService)
                                binding.connectHc05.text = "DISCONNETTI"
                                binding.statusHc05.text = "${device?.name} connesso con successo"
                                alreadyConnected = true
                            } else {
                                binding.statusHc05.text = "${device?.name} non è stato connesso"
                            }
                        }
                    }
                }
            }
        }

        var redToggle = false
        setBgColor(binding.buttonLeft, getColor(R.color.red))
        binding.buttonLeft.setOnTouchListener { view, event -> startAnimation(view, event, true) }
        binding.buttonLeft.setOnClickListener {
            if (writer == null) {
                Toast.makeText(this, "Connettersi al dispositivo HC-05 prima", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            redToggle = !redToggle
            if (redToggle) {
                binding.textToApply.text = "Attivando LED rosso" // "Girando a sinistra di 90°"
                binding.textToApplyBt.text = "R255"
                writer!!.write('R')
            } else {
                binding.textToApply.text = "Disattivando LED rosso" // "Ritornando a 0°"
                binding.textToApplyBt.text = "R000"
                writer!!.write('r')
            }
        }
        // gotta add the same thing on the images too for UX, because that's the important thing
        /*binding.buttonLeftImage.setOnTouchListener { _, event -> startAnimation(binding.buttonLeft, event) }
        binding.buttonLeftImage.setOnClickListener {
            binding.textToApply.text = "Girando a sinistra di 90°"
            binding.textToApplyBt.text = "P-090"
        }*/

        var greenToggle = false
        setBgColor(binding.buttonCenter, getColor(R.color.green))
        binding.buttonCenter.setOnTouchListener { view, event -> startAnimation(view, event, true) }
        binding.buttonCenter.setOnClickListener {
            if (writer == null) {
                Toast.makeText(this, "Connettersi al dispositivo HC-05 prima", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            greenToggle = !greenToggle
            if (greenToggle) {
                binding.textToApply.text = "Attivando LED verde" // "Ritornando a 0°"
                writer!!.write('G')
            } else {
                binding.textToApply.text = "Disattivando LED verde" // "Ritornando a 0°"
                writer!!.write('g')
            }
        }
        /*binding.buttonCenterImage.setOnTouchListener { _, event -> startAnimation(binding.buttonCenter, event) }
        binding.buttonCenterImage.setOnClickListener {
            binding.textToApply.text = "Ritornando a 0°"
            binding.textToApplyBt.text = "P000"
        }*/

        var blueToggle = false
        setBgColor(binding.buttonRight, getColor(R.color.blue))
        binding.buttonRight.setOnTouchListener { view, event -> startAnimation(view, event, true) }
        binding.buttonRight.setOnClickListener {
            if (writer == null) {
                Toast.makeText(this, "Connettersi al dispositivo HC-05 prima", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            blueToggle = !blueToggle
            if (blueToggle) {
                binding.textToApply.text = "Attivando LED blu" // "Girando a destra di 90°"
                writer!!.write('B')
            } else {
                binding.textToApply.text = "Disattivando LED blu" // "Ritornando a 0°"
                writer!!.write('b')
            }
        }
        /*binding.buttonRightImage.setOnTouchListener { _, event -> startAnimation(binding.buttonRight, event) }
        binding.buttonRightImage.setOnClickListener {
            binding.textToApply.text = "Girando a destra di 90°"
            binding.textToApplyBt.text = "P090"
        }*/

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

        binding.sendText.setOnClickListener {
            if (writer == null) {
                Toast.makeText(this, "Connettersi al dispositivo HC-05 prima", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.textToBluetooth.text.isNotEmpty()) {
                writer!!.write('ì')
                writer!!.write(binding.textToBluetooth.text.toString())
                writer!!.write('\n')
            }
        }

        var isExpanded = false
        binding.collapseBar.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                expandView(binding.collapseContent)
                binding.arrowIcon.animate().rotation(180f).setInterpolator(
                    AccelerateDecelerateInterpolator()
                ).start()
            } else {
                collapseView(binding.collapseContent)
                binding.arrowIcon.animate().rotation(0f).setInterpolator(
                    AccelerateDecelerateInterpolator()
                ).start()
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

    private fun setBgColor(view: View, color: Int) {
        val drawable = view.background as GradientDrawable
        drawable.setColor(color)
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

    private fun expandView(view: View) {
        view.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val targetedHeight = view.measuredHeight
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetedHeight)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun collapseView(view: View) {
        val initialHeight = view.measuredHeight

        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
            if (value == 0)
                view.visibility = View.GONE
        }
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun isValidHex(color: String): Boolean =
        color.matches("^#[0-9A-Fa-f]{6}$".toRegex())

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
}
