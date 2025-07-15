package com.example.sensor_app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream

class MainActivity : FlutterActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private lateinit var udpThread: Thread
    private lateinit var wakeLock: PowerManager.WakeLock

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
                    val d = osc("/rotation", arrayOf(rotation.x, rotation.y, rotation.z))
                    val packet = DatagramPacket(d, d.size, InetAddress.getByName(ip), 8000)
                    try {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "SensorApp::WakeLock")
        wakeLock.acquire()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        sensorManager.unregisterListener(this)
    }

    fun osc(addr: String, args: Array<Any>): ByteArray {
        val out = ByteArrayOutputStream()

        oscString(out, addr)
        oscString(out, args.map { oscType(it) }.joinToString(""))
        for (arg in args) when (arg) {
            is String -> oscString(out, arg)
            is Float -> out.write(ByteBuffer.allocate(4).putFloat(arg).array())
            is Int -> out.write(ByteBuffer.allocate(4).putInt(arg).array())
        }
    
        return out.toByteArray()
    }

    fun oscString(out: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray()
        out.write(bytes, 0, bytes.size)
        out.write(0)
        repeat((4 - (bytes.size + 1) % 4) % 4) { out.write(0) }
    }

    fun oscType(x: Any): Char {
        return when (x) {
            is String -> 's'
            is Int -> 'i'
            is Float -> 'f'
            else -> error("Invalid OSC argument type")
        }
    }
}

data class RotationVector(val x: Float, val y: Float, val z: Float)
