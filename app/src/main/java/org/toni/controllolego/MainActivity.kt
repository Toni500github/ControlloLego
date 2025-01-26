package org.toni.controllolego

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
            setViewColor()
        }
        binding.greenSlider.addOnChangeListener { _, value, _ ->
            binding.greenColor.text = value.toInt().toString()
            setViewColor()
        }
        binding.blueSlider.addOnChangeListener { _, value, _ ->
            binding.blueColor.text = value.toInt().toString()
            setViewColor()
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
        binding.brightnessSlideBar.setOnClickListener { view ->
            view.parent.requestDisallowInterceptTouchEvent(true)
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
            if (!binding.colorPickerHex.text.contentEquals("#${envelope.hexCode}") && fromUser)
                binding.colorPickerHex.setText("#${envelope.hexCode}")

            binding.customColorView.setBackgroundColor(envelope.color)
        })

        binding.colorPickerView.attachBrightnessSlider(binding.brightnessSlideBar)
    }

    private fun setViewColor() {
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
        color.matches("^#[0-9A-Fa-f]{8}$".toRegex())
}