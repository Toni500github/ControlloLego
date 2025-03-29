/*
 * Copyright 2024 Toni500git
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.toni.controllolego

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.toni.controllolego.databinding.ActivityMainBinding
import org.toni.controllolego.databinding.BtPairedDevicesBinding
import org.toni.controllolego.databinding.BtPairedDevicesListLayoutBinding

@SuppressLint("MissingPermission")
class BtPairedDevices(val btStuff: BtStuff) : Fragment() {

    private var _binding: BtPairedDevicesBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BtPairedDevicesBinding.inflate(inflater, container, false)
        binding.toolbar.apply {
            setNavigationIcon(R.drawable.arrow_left)
            setNavigationOnClickListener { _ ->
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        if (btStuff.pairedDevices.isNullOrEmpty())
            return binding.root

        for (device in btStuff.pairedDevices!!) {
            Log.d("BluetoothTestingLmao", "device name = ${device.name}")
            Log.d("BluetoothTestingLmao", "MAC address = ${device.address}")
            val deviceButtonLayout = BtPairedDevicesListLayoutBinding.inflate(inflater, container, false)

            deviceButtonLayout.deviceNameView.text = device.name
            deviceButtonLayout.deviceAddressView.text = device.address

            deviceButtonLayout.deviceButton.setOnTouchListener { view, event ->
                context?.let { startAnimation(it, view, event) }
                false
            }
            deviceButtonLayout.deviceButton.setOnClickListener {
                setClickBtDevice(btStuff.bindingMainActivity, context, device)
                requireActivity().supportFragmentManager.popBackStack()
            }

            // Add the LinearLayout to the container
            binding.btDeviceList.addView(deviceButtonLayout.root)
        }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    fun setClickBtDevice(binding: ActivityMainBinding?, context: Context?, device: BluetoothDevice?) {
        if (true) {
            binding!!.statusHc05.text = "Connettendomi al ${device.name}..."
            binding.statusHc05.setTextColor(0xFF8F9099.toInt())
            val uuids = device.uuids
            if (uuids != null) {
                val config = BluetoothConfiguration()
                config.bluetoothServiceClass = BluetoothClassicService::class.java //  BluetoothClassicService.class or BluetoothLeService.class
                config.context = context
                config.bufferSize = 2048
                config.characterDelimiter = '\n'
                config.deviceName = "Controllo Lego"
                config.callListenersInMainThread = true
                config.uuid = uuids[1].uuid
                BluetoothService.init(config)
                btStuff.service = BluetoothService.getDefaultInstance()
                btStuff.service!!.connect(device)
                lifecycleScope.launch {
                    // give it the time to at least set the statusHcO5 text
                    Handler().postDelayed({
                        while (btStuff.service?.status == BluetoothStatus.CONNECTING) {}
                        if (btStuff.service?.status == BluetoothStatus.CONNECTED) {
                            btStuff.writer = BluetoothWriter(btStuff.service)
                            binding.connectHc05.text = "DISCONNETTI"
                            binding.statusHc05.text = "${device.name} connesso con successo"
                            binding.statusHc05.setTextColor(Color.GREEN)
                            btStuff.connected = true
                        } else {
                            binding.statusHc05.text = "Non si è riusciti a connettere L'${device.name}"
                            binding.statusHc05.setTextColor(Color.RED)
                            btStuff.connected = false
                        }
                    }, 500)
                    cancel()
                }
            }
        } else {
            binding?.statusHc05?.text = "Dispositivo '${device.name}' non sembra essere un simile HC"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}