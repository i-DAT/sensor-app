package com.example.sensor_app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder   

class MainActivity : FlutterActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private lateinit var udpThread: Thread

    @Volatile
    private var ip: String? = null
    @Volatile
    private var rotation: RotationVector = RotationVector(0f, 0f, 0f)

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "gyro_udp_channel")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "setTargetAddress" -> ip = call.argument<String>("ip")
                    else -> result.notImplemented()
                }
            }

        Thread {
            val udpSocket = DatagramSocket();
            try {
                while (true) {
                    Thread.sleep(1)
                    if (ip == null) continue;
                    try {
                        // TODO: hard coding OSC messages is messy
                        val address = byteArrayOf(
                            0x2F, 0x72, 0x6F, 0x74,  // "/rot"
                            0x61, 0x74, 0x69, 0x6F,  // "atio"
                            0x6E, 0x00, 0x00, 0x00   // "n\0\0\0"
                        )
                        val typeTag = byteArrayOf(
                            0x2C, 0x66, 0x66, 0x66,  // ",fff"
                            0x00, 0x00, 0x00, 0x00   // "\0\0\0\0"
                        )
                        val floatBuffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
                        floatBuffer.putFloat(rotation.x)
                        floatBuffer.putFloat(rotation.y)
                        floatBuffer.putFloat(rotation.z)
                        val data = address + typeTag + floatBuffer.array()
                        val packet = DatagramPacket(data, data.size, InetAddress.getByName(ip), 8000)
                        udpSocket?.send(packet)
                    } catch (e: Exception) {
                        Log.e("UDP", "Send failed: ${e}")
                    }
                }
            } finally {
                udpSocket.close();
            }
        }.start();
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        rotation = RotationVector(orientation[0], orientation[1], orientation[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

data class RotationVector(val x: Float, val y: Float, val z: Float)
