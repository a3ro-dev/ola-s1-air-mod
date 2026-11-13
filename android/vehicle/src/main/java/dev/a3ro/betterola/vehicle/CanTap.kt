package dev.a3ro.betterola.hud

import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CountDownLatch

/**
 * Listens to the head unit's own CAN tap (wifican.WifiCanRpc/CanDumpStream) on the loopback port.
 *
 * This is a listen-only server stream: one empty request, then frames come back.
 * The class has no way to write a frame and never will; keep it that way.
 */
class CanTap(private val host: String = "127.0.0.1", private val port: Int = 50005) {

    private object Bytes : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)
        override fun parse(stream: InputStream): ByteArray = stream.readBytes()
    }

    private val dump: MethodDescriptor<ByteArray, ByteArray> =
        MethodDescriptor.newBuilder<ByteArray, ByteArray>()
            .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("wifican.WifiCanRpc", "CanDumpStream"))
            .setRequestMarshaller(Bytes)
            .setResponseMarshaller(Bytes)
            .build()

    /** Field 1 (bytes) of CANPacket: tag 0x0A, varint length, payload. */
    private fun payload(packet: ByteArray): ByteArray? {
        var i = 0
        while (i < packet.size) {
            val key = packet[i++].toInt() and 0xFF
            if (key == 0x0A) {
                var len = 0
                var shift = 0
                while (i < packet.size) {
                    val b = packet[i++].toInt() and 0xFF
                    len = len or ((b and 0x7F) shl shift)
                    if (b and 0x80 == 0) break
                    shift += 7
                }
                return if (i + len <= packet.size) packet.copyOfRange(i, i + len) else null
            }
            return null // any other field layout is not a CANPacket
        }
        return null
    }

    /** Runs until [scope] is cancelled, reconnecting with a short backoff. */
    fun start(scope: CoroutineScope, onFrame: (Frame) -> Unit, onConnected: (Boolean) -> Unit): Job =
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var wait = 1_000L
            while (isActive) {
                val channel: ManagedChannel = OkHttpChannelBuilder.forAddress(host, port)
                    .usePlaintext().maxInboundMessageSize(1 shl 20).build()
                val done = CountDownLatch(1)
                try {
                    val call = channel.newCall(dump, CallOptions.DEFAULT)
                    ClientCalls.asyncServerStreamingCall(call, ByteArray(0), object : StreamObserver<ByteArray> {
                        override fun onNext(value: ByteArray) {
                            wait = 1_000L
                            payload(value)?.let { parseFrame(it) }?.let(onFrame)
                        }
                        override fun onError(t: Throwable) { done.countDown() }
                        override fun onCompleted() { done.countDown() }
                    })
                    onConnected(true)
                    while (isActive && !done.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)) { /* wait */ }
                } catch (_: Exception) {
                    // fall through to retry
                } finally {
                    onConnected(false)
                    channel.shutdownNow()
                }
                delay(wait)
                wait = (wait * 2).coerceAtMost(8_000L)
            }
        }
}
