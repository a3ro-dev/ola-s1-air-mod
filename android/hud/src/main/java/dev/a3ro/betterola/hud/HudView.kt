package dev.a3ro.betterola.hud

import dev.a3ro.betterola.vehicle.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * One view draws the whole HUD: edge glows for the indicators, a compact island with the
 * mode, speed and charge that expands for messages, and a power/regen line along the bottom.
 * It never takes touches. It redraws only while something is changing, so Android Auto keeps the frame budget.
 */
class HudView(context: Context) : View(context) {

    @Volatile var state = HudState()
        set(v) { field = v; dirty = true; postInvalidateOnAnimation() }
    @Volatile var stale = true
        set(v) { field = v; dirty = true; postInvalidateOnAnimation() }

    private var dirty = true
    private val signal = Color.rgb(0xFF, 0xD2, 0x3D)   // indicators and warnings
    private val ember = Color.rgb(0xFF, 0x8A, 0x3D)    // power draw
    private val ink = Color.rgb(0xEC, 0xEE, 0xF0)
    private val ink2 = Color.rgb(0xA6, 0xAB, 0xB1)
    private val ground = Color.rgb(0x0B, 0x0C, 0x0E)
    private val fault = Color.rgb(0xFF, 0x5A, 0x4E)
    private val ok = Color.rgb(0x7C, 0xFF, 0x67)

    private val wide: Typeface = font(650, 125)
    private val body: Typeface = font(520, 100)

    private fun font(weight: Int, width: Int): Typeface = try {
        Typeface.Builder(context.assets, "archivo.ttf")
            .setFontVariationSettings("'wght' $weight, 'wdth' $width").build() ?: Typeface.DEFAULT_BOLD
    } catch (_: Exception) { Typeface.DEFAULT_BOLD }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint()
    private var left: Bitmap? = null
    private var right: Bitmap? = null
    private var aL = 0f
    private var aR = 0f
    private var open = 0f           // island expansion 0 compact .. 1 message
    private var lastMsg: Message? = null
    private var lastTick = SystemClock.uptimeMillis()
    private var phase = 0f          // breathing phase, radians
    private var breathNow = 0.5f
    private var band: Bitmap? = null
    private val tint = Paint()
    private val blue = Color.rgb(0x3D, 0xA5, 0xFF)
    private var lastMode = DriveMode.Unknown
    private var modeUntil = 0L
    // The brake lever voltage has no documented rest or full-pull value, so learn both while riding.
    private var brakeLo = Double.NaN
    private var brakeHi = Double.NaN
    private val rect = RectF()
    private val bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val gw = (w * 0.16f).toInt().coerceAtLeast(40)
        band = band(w, (h * 0.16f).toInt().coerceAtLeast(24))
        left = glow(gw, h, mirror = false)
        right = glow(gw, h, mirror = true)
    }

    /** White dither that thins toward the top. Tinted at draw time, so one bitmap serves every colour. */
    private fun band(w: Int, h: Int): Bitmap {
        val cell = max(2, (resources.displayMetrics.density * 1.5f).toInt())
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val u = (h - 1 - y) / h.toFloat()          // 0 at the bottom edge, 1 at the top of the band
            val density = (1f - u).pow(1.6f)
            for (x in 0 until w) {
                val t = bayer[((y / cell) % 4) * 4 + ((x / cell) % 4)] / 16f
                if (t < density) px[y * w + x] = Color.WHITE
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return bmp
    }

    /** An ordered-dither gradient, drawn once into a bitmap. No per-frame blur, no gradients to composite. */
    private fun glow(w: Int, h: Int, mirror: Boolean): Bitmap {
        val cell = max(2, (resources.displayMetrics.density * 1.5f).toInt())
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val u = (if (mirror) w - 1 - x else x) / w.toFloat()
            val density = (1f - u).pow(1.7f)
            val t = bayer[((y / cell) % 4) * 4 + ((x / cell) % 4)] / 16f
            if (t < density) px[y * w + x] = signal
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return bmp
    }

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = min(64L, now - lastTick) / 1000f
        lastTick = now
        val s = state
        val stale = stale
        if (s.mode != lastMode) {
            if (lastMode != DriveMode.Unknown && s.mode != DriveMode.Unknown) modeUntil = now + 1600
            lastMode = s.mode
        }
        val msg = message(s, stale)
            ?: if (now < modeUntil) Message(when (s.mode) { DriveMode.Reverse -> "Reverse"; DriveMode.Parked -> "Park"; else -> s.mode.label + " mode" }, Level.Info) else null
        if (msg != null) lastMsg = msg
        if (now < modeUntil) postInvalidateOnAnimation()

        // ease toward targets; with animations off the system scale is 0 and we snap
        val k = if (android.provider.Settings.Global.getFloat(context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) 1f else min(1f, dt * 14f)
        val tL = if (s.leftIndicator) 1f else 0f
        val tR = if (s.rightIndicator) 1f else 0f
        aL += (tL - aL) * k
        aR += (tR - aR) * k
        open += ((if (msg != null) 1f else 0f) - open) * k

        // Breathing: slow when parked, quicker and fuller as speed and power rise. Frozen when animations are off.
        val animOff = k >= 1f
        val speed = s.speedKmh ?: 0
        val amps = s.motorAmps ?: 0.0
        val effort = max(min(speed, 60) / 60f, min(abs(amps).toFloat(), 80f) / 80f)
        if (!animOff) phase += dt * (0.20f + 0.55f * effort) * 6.2832f
        val breath = if (animOff) 0.5f else 0.5f + 0.5f * sin(phase)
        breathNow = breath
        val ride = speed > 2 || abs(amps) > 5
        val ambient = when { s.brake -> fault; amps < -3 -> ok; ride -> ember; else -> ink2 }
        val strength = (0.10f + 0.26f * breath) * (1f + 0.9f * effort)
        band?.let {
            tint.colorFilter = PorterDuffColorFilter(ambient, PorterDuff.Mode.SRC_IN)
            tint.alpha = (strength.coerceAtMost(0.8f) * 255).toInt()
            c.drawBitmap(it, 0f, height - it.height.toFloat(), tint)
        }
        val edge = (0.05f + 0.09f * breath) * (1f + effort)
        tint.colorFilter = PorterDuffColorFilter(ink2, PorterDuff.Mode.SRC_IN)
        tint.alpha = (edge * 255).toInt()
        if (aL < 0.05f) left?.let { c.drawBitmap(it, 0f, 0f, tint) }
        if (aR < 0.05f) right?.let { c.drawBitmap(it, width - it.width.toFloat(), 0f, tint) }

        left?.let { glowPaint.alpha = (aL * 235).toInt(); if (aL > 0.02f) c.drawBitmap(it, 0f, 0f, glowPaint) }
        right?.let { glowPaint.alpha = (aR * 235).toInt(); if (aR > 0.02f) c.drawBitmap(it, width - it.width.toFloat(), 0f, glowPaint) }

        drawIsland(c, s, stale)
        drawPowerLine(c, s)

        val moving = abs(aL - tL) > 0.02f || abs(aR - tR) > 0.02f || abs(open - (if (msg != null) 1f else 0f)) > 0.02f
        dirty = false
        if (moving) postInvalidateOnAnimation()
        else if (!animOff) postInvalidateDelayed(33)   // about 30 fps is plenty for a slow breath
    }

    private fun drawIsland(c: Canvas, s: HudState, stale: Boolean) {
        val d = resources.displayMetrics.density
        val h = 40f * d
        val compactW = 276f * d
        val msg = lastMsg
        val textW = if (msg != null) { p.typeface = body; p.textSize = 15f * d; p.measureText(msg.text) + 40f * d } else 0f
        val w = compactW + (max(compactW, textW) - compactW) * open
        // Left-aligned on purpose: Android Auto puts the Maps search bar and its app tabs in the top centre.
        val left = 14f * d
        val top = 10f * d
        rect.set(left, top, left + w, top + h)
        p.style = Paint.Style.FILL
        p.color = ground
        c.drawRoundRect(rect, h / 2, h / 2, p)

        // compact content fades out as the message opens
        val compactA = ((1f - open) * 255).toInt()
        if (compactA > 8) {
            drawLamp(c, rect.left + 19f * d, rect.centerY(), 8f * d, s, compactA)
            p.typeface = wide
            p.textSize = 18f * d
            p.color = withAlpha(ink, compactA)
            p.textAlign = Paint.Align.LEFT
            val speed = s.speedKmh?.toString() ?: "--"
            val sx = rect.left + 36f * d
            c.drawText(speed, sx, rect.centerY() + 6.5f * d, p)
            val sw = p.measureText("88")
            p.typeface = body; p.textSize = 11f * d; p.color = withAlpha(ink2, compactA)
            c.drawText("km/h", sx + 4f * d + sw, rect.centerY() + 4f * d, p)
            p.textAlign = Paint.Align.CENTER
            p.textSize = 13f * d
            c.drawText(s.mode.label, rect.centerX(), rect.centerY() + 4.5f * d, p)
            // Charge and range together: how far the scooter will still go is the number that matters on a ride.
            p.textAlign = Paint.Align.RIGHT
            p.typeface = wide; p.textSize = 15f * d
            p.color = withAlpha(if ((s.socPercent ?: 100) <= 15) fault else ink, compactA)
            val socText = s.socPercent?.let { "$it%" } ?: "--%"
            val socW = p.measureText(socText)
            c.drawText(socText, rect.right - 16f * d, rect.centerY() + 5.5f * d, p)
            val range = s.rangeKm?.let { "· $it km" } ?: ""
            if (range.isNotEmpty()) {
                p.typeface = body; p.textSize = 11f * d; p.color = withAlpha(ink2, compactA)
                c.drawText(range, rect.right - 18f * d - socW, rect.centerY() + 4f * d, p)
            }
            p.textAlign = Paint.Align.LEFT
        }
        if (msg != null && open > 0.05f) {
            p.typeface = body; p.textSize = 15f * d
            p.color = withAlpha(when (msg.level) { Level.Fault -> fault; Level.Warn -> signal; Level.Info -> ink }, (open * 255).toInt())
            p.textAlign = Paint.Align.CENTER
            c.drawText(msg.text, rect.centerX(), rect.centerY() + 5.5f * d, p)
            p.textAlign = Paint.Align.LEFT
        }
    }

    /** Power and regen ambience: a thin dithered line, orange when drawing power, green when regenerating, red on brake. */
    /** A headlamp: low beam = green, rays angled down; high beam ("deeper") = blue, rays level. Nothing when off. */
    private fun drawLamp(c: Canvas, x: Float, y: Float, r: Float, s: HudState, alpha: Int) {
        if (!s.lowBeam && !s.highBeam) return
        val high = s.highBeam
        val col = withAlpha(if (high) blue else ok, alpha)
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.22f
        p.strokeCap = Paint.Cap.ROUND
        p.color = col
        rect.set(x - r, y - r, x + r * 0.2f, y + r)
        c.drawArc(rect, 90f, 180f, false, p)                 // lamp body, open to the right
        val sx = x + r * 0.55f
        for (i in -1..1) {
            val ry = y + i * r * 0.62f
            val dy = if (high) 0f else r * 0.55f             // low beam rays tilt down
            c.drawLine(sx, ry, sx + r * 1.0f, ry + dy, p)
        }
        p.style = Paint.Style.FILL
    }

    private fun brakeLevel(s: HudState): Float {
        val v = s.brakeVolts ?: return if (s.brake) 1f else 0f
        if (brakeLo.isNaN() || v < brakeLo) brakeLo = v
        if (brakeHi.isNaN() || v > brakeHi) brakeHi = v
        val span = brakeHi - brakeLo
        if (span < 0.3) return if (s.brake) 1f else 0f   // not enough range seen yet
        return ((v - brakeLo) / span).toFloat().coerceIn(0f, 1f)
    }

    /**
     * The bottom meter, origin at the centre, in the same ordered dither as everything else.
     * Power draw grows to the right in orange, regeneration grows to the left in green, and the brake
     * fills both sides in red (denser the harder the pull). Density is intensity and the whole meter breathes.
     * Easy Park, the scooter's reverse, which also rolls forward, hatches it.
     */
    private fun drawPowerLine(c: Canvas, s: HudState) {
        val d = resources.displayMetrics.density
        val amps = s.motorAmps ?: 0.0
        val brake = brakeLevel(s).let { if (s.brake) max(it, 0.35f) else it }
        val power = if (amps > 0) (amps / 80.0).coerceIn(0.0, 1.0).toFloat() else 0f
        val regen = if (amps < 0) (-amps / 40.0).coerceIn(0.0, 1.0).toFloat() else 0f   // regen runs at lower current
        val cx = width / 2
        val cell = max(2, (d * 1.5f).toInt())
        val rows = 4
        val maxLen = (width * 0.42f).toInt()
        val easyPark = s.mode == DriveMode.Reverse
        p.style = Paint.Style.FILL

        fun side(dir: Int, level: Float, color: Int) {
            if (level < 0.03f) return
            val len = (maxLen * (0.15f + 0.85f * level)).toInt()
            p.color = color
            var o = 0
            while (o < len) {
                val x = if (dir > 0) cx + o else cx - o - cell
                val fall = (1f - o / len.toFloat()).coerceAtLeast(0.12f)      // thins toward the far end
                val density = (0.30f + 0.70f * level) * (0.85f + 0.15f * breathNow) * fall
                for (r in 0 until rows) {
                    val t = bayer[(r % 4) * 4 + ((x / cell) % 4)] / 16f
                    val hatch = easyPark && ((x / cell + r) % 3 != 0)
                    if (t < density && !hatch) {
                        val y = height - (rows - r) * cell.toFloat()
                        c.drawRect(x.toFloat(), y, (x + cell).toFloat(), y + cell, p)
                    }
                }
                o += cell
            }
        }

        if (brake > 0.03f) {
            side(1, brake, fault); side(-1, brake, fault)
        } else {
            side(1, power, ember)
            side(-1, regen, ok)
        }

        // Words as well as colour, so the meter never depends on hue alone.
        val label = when { brake > 0.03f -> null; regen >= 0.03f -> "REGEN" to -1; power >= 0.03f -> "POWER" to 1; else -> null }
        if (label != null) {
            p.typeface = body; p.textSize = 10f * d
            p.color = withAlpha(ink2, 200)
            p.textAlign = if (label.second > 0) Paint.Align.LEFT else Paint.Align.RIGHT
            c.drawText(label.first, cx + label.second * 10f * d, height - rows * cell - 6f * d, p)
            p.textAlign = Paint.Align.LEFT
        }
    }

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
}
