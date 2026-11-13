package dev.a3ro.betterola.vehicle

import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.ClientCalls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * The stock HMI command channel: CAN 0x6BC, byte 0 is the CommandFromHMI ordinal.
 * The head unit's own HMI writes it with `cansend` on vcan0, and the MCU only bridges
 * those writes while the scooter is awake. Nothing here runs unless a button asks for it.
 */
object HmiCommand {
    const val LOCK = 0x54      // Lock_Now
    const val UNLOCK = 0x19    // PASSCODE_AUTH; the stock unlock loop repeats it 30x at 100 ms
    const val BOOT = 0x03      // Seat_Unlock_Cmd

    private object Bytes : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)
        override fun parse(stream: InputStream): ByteArray = stream.readBytes()
    }

    private val method: MethodDescriptor<ByteArray, ByteArray> =
        MethodDescriptor.newBuilder<ByteArray, ByteArray>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("wifican.WifiCanRpc", "ExecuteADBCommand"))
            .setRequestMarshaller(Bytes)
            .setResponseMarshaller(Bytes)
            .build()

    /** Sends [cmd] [times] times. Returns false on a transport error or a non-success status. */
    suspend fun send(cmd: Int, times: Int = 1, gapMs: Long = 100L): Boolean = withContext(Dispatchers.IO) {
        val channel = OkHttpChannelBuilder.forAddress("127.0.0.1", 50005).usePlaintext().build()
        try {
            val hex = Integer.toHexString(cmd).uppercase().padStart(2, '0')
            val command = "cansend vcan0 6BC#${hex}00000000000000"
            for (i in 0 until times) {
                if (!call(channel, command)) return@withContext false
                if (i < times - 1) delay(gapMs)
            }
            true
        } finally {
            channel.shutdownNow()
        }
    }

    private fun call(channel: ManagedChannel, command: String): Boolean = try {
        val reply = ClientCalls.blockingUnaryCall(
            channel, method, CallOptions.DEFAULT.withDeadlineAfter(8, TimeUnit.SECONDS), payload(command)
        )
        status(reply) in 0..1
    } catch (_: Exception) {
        false
    }

    private fun payload(command: String): ByteArray {
        val raw = command.toByteArray(StandardCharsets.UTF_8)
        return byteArrayOf(0x0A) + varint(raw.size.toLong()) + raw
    }

    private fun varint(value: Long): ByteArray {
        var v = value
        val out = ArrayList<Byte>(8)
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) out.add((b or 0x80).toByte()) else { out.add(b.toByte()); break }
        }
        return out.toByteArray()
    }

    /** ExecuteADBCommand reply: field 1 is the status varint, field 2 is the message. */
    private fun status(buf: ByteArray): Int {
        var i = 0
        while (i < buf.size) {
            var key = 0L
            var shift = 0
            while (i < buf.size) {
                val b = buf[i++].toInt() and 0xFF
                key = key or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            when ((key and 7).toInt()) {
                0 -> {
                    var value = 0L
                    shift = 0
                    while (i < buf.size) {
                        val b = buf[i++].toInt() and 0xFF
                        value = value or ((b and 0x7F).toLong() shl shift)
                        if (b and 0x80 == 0) break
                        shift += 7
                    }
                    if ((key ushr 3).toInt() == 1) return value.toInt()
                }
                2 -> {
                    var len = 0
                    shift = 0
                    while (i < buf.size) {
                        val b = buf[i++].toInt() and 0xFF
                        len = len or ((b and 0x7F) shl shift)
                        if (b and 0x80 == 0) break
                        shift += 7
                    }
                    i += len
                }
                else -> return -1
            }
        }
        return -1
    }
}
