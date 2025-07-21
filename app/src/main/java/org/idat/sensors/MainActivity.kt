package org.idat.sensors

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.idat.sensors.ui.theme.SensorsTheme
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.BlockingQueue

class MainActivity : ComponentActivity() {
    lateinit var outBuffer: RingBuffer<Any>

    @Volatile
    var ip: String? = "10.254.161.3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        outBuffer = RingBuffer(128)
        Thread(::send).start()
        setContent {
            SensorsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MsgButton(outBuffer)
                }
            }
        }
    }

    fun send() {
        DatagramSocket().use { sock ->
            while (true) {
                val data = outBuffer.take()
                Log.d("SEND", "Got data")
                if (ip == null) continue;
                val packet = serialize(data)
                try {
                    sock.send(DatagramPacket(packet, packet.size, InetAddress.getByName(ip), 8000))
                    Log.d("SEND", "Sent")
                } catch (e: IOException) {
                    Log.e("UDP", e.toString())
                }
            }
        }
    }
}

@Composable
fun MsgButton(buffer: RingBuffer<Any>) {


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            buffer.put(ExampleMsg(5.3f, "bar"))
        }) {
            Text("Click Me")
        }
    }
}

data class ExampleMsg(val foo: Float, val bar: String)