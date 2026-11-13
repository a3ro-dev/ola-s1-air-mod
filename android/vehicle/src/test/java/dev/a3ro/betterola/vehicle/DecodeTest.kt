package dev.a3ro.betterola.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeTest {
    private fun frame(id: Int, vararg b: Int) =
        Frame(id, ByteArray(8) { if (it < b.size) b[it].toByte() else 0 })

    @Test fun bitsReadsIntelFields() {
        val d = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00) // bits 40 and 41 set
        assertEquals(1, bits(d, 40, 1))
        assertEquals(1, bits(d, 41, 1))
        assertEquals(3, bits(d, 40, 2))
        assertEquals(0, bits(d, 42, 1))
    }

    @Test fun parsesBigEndianIdAndData() {
        val raw = ByteArray(16)
        raw[2] = 0x03; raw[3] = 0x53.toByte(); raw[4] = 8
        raw[8] = 0x7F
        val f = parseFrame(raw)!!
        assertEquals(0x353, f.id)
        assertEquals(0x7F, f.data[0].toInt())
        assertNull(parseFrame(ByteArray(8)))
    }

    @Test fun indicatorsComeFromBits40And41() {
        // bit 40 = right, bit 41 = left, both live in byte 5
        val s = HudState().apply(frame(Ids.DISPLAY_INFO, 0, 0, 0, 0, 0, 0b01), 1)
        assertTrue(s.rightIndicator); assertFalse(s.leftIndicator)
        val h = HudState().apply(frame(Ids.DISPLAY_INFO, 0, 0, 0, 0, 0, 0b11), 1)
        assertTrue(h.hazard)
    }

    @Test fun speedSocAndMode() {
        var s = HudState()
        s = s.apply(frame(Ids.DISPLAY_SPEED, 42), 1)
        s = s.apply(frame(Ids.DISPLAY_SOC, 64, 0), 1)
        assertEquals(42, s.speedKmh); assertEquals(64, s.socPercent)
        // Vehicle_Discharge_Mode: bits 41..44, so bit 1 of byte 5 up to bit 4
        val sport = HudState().apply(frame(Ids.DISCHARGE_MODE, 0, 0, 0, 0, 0, 3 shl 1), 1)
        assertEquals(DriveMode.Sport, sport.mode)
        val rev = HudState().apply(frame(Ids.DISCHARGE_MODE, 0, 0, 0, 0, 0, 5 shl 1), 1)
        assertEquals(DriveMode.Reverse, rev.mode)
    }

    @Test fun unknownFramesLeaveStateAlone() {
        val s = HudState()
        assertTrue(s === s.apply(frame(0x7FF, 1, 2, 3), 1))
    }

    @Test fun messagesPrioritiseFaultsAndStale() {
        assertEquals("Waiting for the scooter", message(HudState(), stale = true)?.text)
        val moving = HudState(speedKmh = 20, sideStandDown = true)
        assertEquals(Level.Fault, message(moving, stale = false)?.level)
        assertNull(message(HudState(speedKmh = 20, sideStandDown = false, unlocked = true), stale = false))
    }
}
