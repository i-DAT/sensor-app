package org.idat.sensors

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RingBuffer<T>(private val capacity: Int) {
    private val buffer = ArrayDeque<T>(capacity)
    private val lock = ReentrantLock()
    private val notEmpty: Condition = lock.newCondition()

    fun put(item: T) {
        lock.withLock {
            if (buffer.size == capacity) {
                buffer.removeFirst() // Overwrite oldest
            }
            buffer.addLast(item)
            notEmpty.signal() // Wake up any thread waiting on take()
        }
    }

    fun take(): T {
        lock.withLock {
            while (buffer.isEmpty()) {
                notEmpty.await()
            }
            return buffer.removeFirst()
        }
    }

    fun size(): Int = lock.withLock { buffer.size }
    fun isEmpty(): Boolean = lock.withLock { buffer.isEmpty() }
    fun isFull(): Boolean = lock.withLock { buffer.size == capacity }
}