package org.idat.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.idat.sensors.ui.theme.SensorsTheme
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket

class MainActivity : ComponentActivity(), SensorEventListener {
    lateinit var outBuffer: RingBuffer<Message>
    lateinit var inBuffer: RingBuffer<Message>

    @Volatile
    var address: InetAddress? = null
    var message: MutableState<String> = mutableStateOf("Waiting for host...")

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        outBuffer = RingBuffer(128)
        inBuffer = RingBuffer(128)
        Thread(::discover).start()
        Thread(::send).start()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)

        setContent {
            SensorsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .then(gestureModifier()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Text(
                                text = message.value,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    fun send() {
        DatagramSocket().use { sock ->
            while (true) {
                val data = outBuffer.take()
                if (address == null) continue
                val packet = serialize(data)
                try {
                    sock.send(DatagramPacket(packet, packet.size, address, 8000))
                } catch (e: IOException) {
                    Log.e("UDP", e.toString())
                }
            }
        }
    }

    fun discover() {
        MulticastSocket(4001).use { sock ->
            sock.joinGroup(InetAddress.getByName("239.255.255.250"))
            val buf = ByteArray(128)
            val packet = DatagramPacket(buf, buf.size)
            while (true) {
                sock.receive(packet)
                val msg = deserialize(buf.copyOf(packet.length))
                if (msg.address == "host") {
                    address = InetAddress.getByName(msg.args[0] as String)
                    runOnUiThread { message.value = "Connected to ${msg.args[1] as String}" }
                    break
                }
            }
        }
        receive()
    }

    fun receive() {
        DatagramSocket().use { sock ->
            val buf = ByteArray(1024)
            while (true) {
                val packet = DatagramPacket(buf, 1024, address, 8000)
                sock.receive(packet)
                inBuffer.put(deserialize(buf.copyOf(packet.length)))
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        outBuffer.put(Message("rotation", arrayOf(orientation[0], orientation[1], orientation[2])))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun gestureModifier(): Modifier {
        val args = { offset: Offset -> arrayOf(offset.x, offset.y) as Array<Any> }
        return Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { outBuffer.put(Message("drag_start", args(it))) },
                    onDragEnd = { outBuffer.put(Message("drag_end")) },
                    onDragCancel = { outBuffer.put(Message("drag_end")) },
                    onDrag = { change, _ ->
                        change.consume()
                        outBuffer.put(Message("drag", args(change.position)))
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { outBuffer.put(Message("single_tap")) },
                    onDoubleTap = { outBuffer.put(Message("double_tap")) },
                    onLongPress = { outBuffer.put(Message("long_press")) },
                )
            }
    }
}
