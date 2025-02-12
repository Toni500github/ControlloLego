package org.toni.controllolego

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter
import org.toni.controllolego.databinding.ActivityMainBinding


@SuppressLint("ClickableViewAccessibility", "MissingPermission", "SetTextI18n")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mService: BluetoothService
    private var writer: BluetoothWriter? = null
    private val BTAdapter = BluetoothAdapter.getDefaultAdapter()

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

        setBtButton(binding.buttonLeft, binding.buttonLeftImage,"Girando a sinistra di 90°", "p090")
        setBtButton(binding.buttonCenter, binding.buttonCenterImage,"Ritornando a 0°", "P000")
        setBtButton(binding.buttonRight, binding.buttonRightImage,"Girando a destra di 90°", "P090")

        binding.sendText.setOnClickListener {
            if (binding.textToBluetooth.text.isNotEmpty())
                writer?.write(binding.textToBluetooth.text.toString()) ?: showHCNotConnected()
        }
    }

    private fun showHCNotConnected() {
        Toast.makeText(this, "Connettersi al dispositivo HC-05 prima", Toast.LENGTH_SHORT).show()
    }
    
    private fun setBtButton(button: View, imageView: ImageView, textToApply: String, textToApplyBt: String) {
        button.setOnTouchListener { view, event -> startAnimation(view, event); startAnimation(imageView, event) }
        button.setOnClickListener {
            binding.textToApply.text =  textToApply
            binding.textToApplyBt.text = textToApplyBt
            writer?.write(textToApplyBt) ?: showHCNotConnected()
        }
        imageView.setOnTouchListener { _, event -> startAnimation(button, event); startAnimation(imageView, event) }
        imageView.setOnClickListener {
            binding.textToApply.text =  textToApply
            binding.textToApplyBt.text = textToApplyBt
            writer?.write(textToApplyBt) ?: showHCNotConnected()
        }
    }

    private fun startAnimation(imageView: ImageView, event: MotionEvent): Boolean {
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
            imageView.background.setTint(animator.animatedValue as Int)
        }
        colorAnimator.start()
        return false
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
