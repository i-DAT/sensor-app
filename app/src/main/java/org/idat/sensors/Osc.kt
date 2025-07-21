package org.idat.sensors

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.reflect.KProperty

fun serialize(obj: Any): ByteArray {
    val stream = ByteArrayOutputStream()

    fun writeString(str: String) {
        val bytes = str.toByteArray()
        stream.write(bytes, 0, bytes.size)
        stream.write(0)
        repeat((4 - (bytes.size + 1) % 4) % 4) { stream.write(0) }
    }

    fun typeCode(obj: Any): Char? {
        return when (obj) {
            is String -> 's'
            is Float -> 'f'
            is Int -> 'i'
            else -> null
        }
    }

    fun String.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2")
            .lowercase()
    }


    val props = obj::class.members
        .filterIsInstance<KProperty<*>>()
        .map { it.getter.call(obj)!! }

    writeString(obj::class.simpleName!!.toSnakeCase())
    writeString(props.map { typeCode(it) }.joinToString(prefix=",", separator = ""))
    for (p in props) when (p) {
        is String -> writeString(p)
        is Float -> stream.write(ByteBuffer.allocate(4).putFloat(p).array())
        is Int -> stream.write(ByteBuffer.allocate(4).putInt(p).array())
        else -> error("Invalid field $p")
    }

    return stream.toByteArray()
}
