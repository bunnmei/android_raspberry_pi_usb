package com.example.usb_cdc

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.USB_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.usb_cdc.ui.theme.USB_CDCTheme
import java.nio.ByteBuffer


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            USB_CDCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

val ACTION_USB_PERMISSION = "hogehoge"
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val manager: MutableState<UsbManager?> = remember {
        mutableStateOf(null)
    }
    val device: MutableState<UsbDevice?> = remember {
        mutableStateOf(null)
    }

    val thread: MutableState<Thread?> = remember {
        mutableStateOf(null)
    }
    val intf: MutableState<UsbInterface?> = remember {
        mutableStateOf(null)
    }
    val connection: MutableState<UsbDeviceConnection?> = remember {
        mutableStateOf(null)
    }
    val epIN: MutableState<UsbEndpoint?> = remember {
        mutableStateOf(null)
    }

    fun getDevice() {
        manager.value = ctx.getSystemService(USB_SERVICE) as? UsbManager
        val usbPermissionIntent = PendingIntent.getBroadcast(
            ctx,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        println(manager.value)
        device.value = manager.value?.deviceList?.values?.first()
        println("hoge2")
        if (device.value != null){
            println("hoge3")
            manager.value?.requestPermission(device.value, usbPermissionIntent)
        }
    }
    fun connect() {

            println("スレッドスタート")
            for (i in 0 until device.value!!.interfaceCount){
                if(device.value!!.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_CDC_DATA) {
                    intf.value = device.value!!.getInterface(i)
                    println(intf.value!!.interfaceClass)

                    for (j in 0 until intf.value!!.endpointCount){
                        if(
                            intf.value!!.getEndpoint(j).type == UsbConstants.USB_ENDPOINT_XFER_BULK
                         && intf.value!!.getEndpoint(j).direction == UsbConstants.USB_DIR_IN
                        ){
                            println("endpint の発見 ${intf.value!!.getEndpoint(j)}")
                            epIN.value = intf.value!!.getEndpoint(j)
                        }
                    }
                }
            }
            connection.value = manager.value!!.openDevice(device.value)
            connection.value!!.claimInterface(intf.value!!, true)

        val baudRate = 115200
        val dataBits = 8
        val stopBits = 0
        val parity = 0

        val msg = byteArrayOf(
            (baudRate and 0xff).toByte(),
            ((baudRate shr 8) and 0xff).toByte(),
            ((baudRate shr 16) and 0xff).toByte(),
            ((baudRate shr 24) and 0xff).toByte(),
            stopBits.toByte(),
            parity.toByte(),
            dataBits.toByte()
        )

        println("msg - $msg")

        val reqType = UsbConstants.USB_TYPE_CLASS or 0x01

        val Req = UsbRequest();
        Req.initialize(connection.value!!, epIN.value!!)

        val cont = connection.value!!.controlTransfer(
            reqType,
            0x20,
            0,
            0,
            msg,
            msg.size,
            5000,
            )
        connection.value!!.controlTransfer(
            reqType,
            0x22,
            0 or 0x1,
            0,
            null,
            0,
            5000,
        )

        thread.value = Thread( Runnable {

              while (true) {
                val buffer = ByteArray(64)
                val byteRead = connection.value!!.bulkTransfer(
                    epIN.value, buffer, buffer.size, 5000)
                if (byteRead > 0) {
                    val seri = String(buffer,0, byteRead)
                    println(seri)
                }
              }

        })
        thread.value!!.start()
    }

    UsbDeviceBroadcastReceiver(
        systemAction = ACTION_USB_PERMISSION,
        onSystemEvent = {}
    ){
        connect()
    }

    Column {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(onClick = { getDevice() }) {
            Text(text = "USBの取得")
        }
    }
}

@Composable
fun UsbDeviceBroadcastReceiver(
    systemAction: String,
    onSystemEvent: (intent: Intent?) -> Unit,
    connect:() -> Unit
) {
    val context = LocalContext.current

    val intentFilter = IntentFilter().apply {
        addAction(systemAction)
    }

    DisposableEffect(context, systemAction) {
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        val manager = context.getSystemService(USB_SERVICE) as? UsbManager
                        val device = manager?.deviceList?.values?.first()
                        val perm = manager?.hasPermission(device)
                        if (perm!!) {
                            connect()
                        }
                    }
                }
            }
        }

        context.registerReceiver(broadcast, intentFilter)

        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    USB_CDCTheme {
        Greeting("Android")
    }
}



//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//
//                    usbRequest.queue(buffer, epIN.value!!.maxPacketSize)
//                    if (connection.value!!.requestWait() == usbRequest){
//                        usbRequest.getClientData();
//                    }
//                }
