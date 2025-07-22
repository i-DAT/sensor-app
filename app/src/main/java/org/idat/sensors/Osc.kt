package org.idat.sensors

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class Message(val address: String, val args: Array<Any> = arrayOf())

fun serialize(msg: Message): ByteArray {
    val s = ByteArrayOutputStream()
    val stream = DataOutputStream(s)

    fun writeString(str: String) {
        val bytes = str.toByteArray()
        stream.write(bytes, 0, bytes.size)
        stream.write(0)
        repeat((4 - (bytes.size + 1) % 4) % 4) { stream.write(0) }
    }

    writeString("/" + msg.address)
    writeString(msg.args.map {
        when (it) {
            is String -> 's'
            is Float -> 'f'
            is Int -> 'i'
            else -> null
        }
    }.joinToString(prefix = ",", separator = ""))
    for (a in msg.args) when (a) {
        is String -> writeString(a)
        is Float -> stream.writeFloat(a)
        is Int -> stream.writeInt(a)
        else -> error("Invalid argument $a")
    }

    return s.toByteArray()
}

fun deserialize(buf: ByteArray): Message {
    val stream = DataInputStream(ByteArrayInputStream(buf))

    fun readString(): String {
        val out = ByteArrayOutputStream()
        while (true) {
            val b = stream.read()
            if (b == 0 || b == -1) break
            out.write(b)
        }
        stream.readNBytes((4 - (out.size() + 1) % 4) % 4)
        return out.toString()
    }

    val address = readString().drop(1)
    val args = mutableListOf<Any>()
    val types = readString()
    for (c in types) when (c) {
        ',' -> {}
        's' -> args.add(readString())
        'f' -> args.add(stream.readFloat())
        'i' -> args.add(stream.readInt())
    }
    return Message(address, args.toTypedArray())
}

