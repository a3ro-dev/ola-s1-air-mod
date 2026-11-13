package dev.a3ro.betterola.hud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
    private val amber = Color.rgb(0xFF, 0xB0, 0x21)
    private val ink = Color.rgb(0xF2, 0xEE, 0xE6)
    private val ink2 = Color.rgb(0xB5, 0xAE, 0x9F)
    private val ground = Color.rgb(0x0F, 0x0E, 0x0C)
    private val fault = Color.rgb(0xFF, 0x5A, 0x4E)
    private val ok = Color.rgb(0x9B, 0xD1, 0x7A)

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
    private var lastMode = DriveMode.Unknown
    private var modeUntil = 0L
    // The brake lever voltage has no documented rest or full-pull value, so learn both while riding.
    private var brakeLo = Double.NaN
    private var brakeHi = Double.NaN
    private val rect = RectF()
    private val bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val gw = (w * 0.16f).toInt().coerceAtLeast(40)
        left = glow(gw, h, mirror = false)
        right = glow(gw, h, mirror = true)
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
            if (t < density) px[y * w + x] = amber
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
            ?: if (now < modeUntil) Message(if (s.mode == DriveMode.Reverse) "Reverse" else s.mode.label + " mode", Level.Info) else null
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

        left?.let { glowPaint.alpha = (aL * 235).toInt(); if (aL > 0.02f) c.drawBitmap(it, 0f, 0f, glowPaint) }
        right?.let { glowPaint.alpha = (aR * 235).toInt(); if (aR > 0.02f) c.drawBitmap(it, width - it.width.toFloat(), 0f, glowPaint) }

        drawIsland(c, s, stale)
        drawPowerLine(c, s)

        val moving = abs(aL - tL) > 0.02f || abs(aR - tR) > 0.02f || abs(open - (if (msg != null) 1f else 0f)) > 0.02f
        if (moving || dirty) { dirty = false; if (moving) postInvalidateOnAnimation() }
    }

    private fun drawIsland(c: Canvas, s: HudState, stale: Boolean) {
        val d = resources.displayMetrics.density
        val h = 40f * d
        val compactW = 232f * d
        val msg = lastMsg
        val textW = if (msg != null) { p.typeface = body; p.textSize = 15f * d; p.measureText(msg.text) + 40f * d } else 0f
        val w = compactW + (max(compactW, textW) - compactW) * open
        val cx = width / 2f
        val top = 10f * d
        rect.set(cx - w / 2, top, cx + w / 2, top + h)
        p.style = Paint.Style.FILL
        p.color = ground
        c.drawRoundRect(rect, h / 2, h / 2, p)

        // compact content fades out as the message opens
        val compactA = ((1f - open) * 255).toInt()
        if (compactA > 8) {
            p.typeface = wide
            p.textSize = 18f * d
            p.color = withAlpha(ink, compactA)
            p.textAlign = Paint.Align.LEFT
            val speed = s.speedKmh?.toString() ?: "--"
            c.drawText(speed, rect.left + 18f * d, rect.centerY() + 6.5f * d, p)
            val sw = p.measureText("88")
            p.typeface = body; p.textSize = 11f * d; p.color = withAlpha(ink2, compactA)
            c.drawText("km/h", rect.left + 22f * d + sw, rect.centerY() + 4f * d, p)
            p.textAlign = Paint.Align.CENTER
            p.textSize = 13f * d
            c.drawText(s.mode.label, cx + 6f * d, rect.centerY() + 4.5f * d, p)
            p.textAlign = Paint.Align.RIGHT
            p.typeface = wide; p.textSize = 15f * d
            p.color = withAlpha(if ((s.socPercent ?: 100) <= 15) fault else ink, compactA)
            c.drawText(s.socPercent?.let { "$it%" } ?: "--%", rect.right - 18f * d, rect.centerY() + 5.5f * d, p)
            p.textAlign = Paint.Align.LEFT
        }
        if (msg != null && open > 0.05f) {
            p.typeface = body; p.textSize = 15f * d
            p.color = withAlpha(when (msg.level) { Level.Fault -> fault; Level.Warn -> amber; Level.Info -> ink }, (open * 255).toInt())
            p.textAlign = Paint.Align.CENTER
            c.drawText(msg.text, cx, rect.centerY() + 5.5f * d, p)
            p.textAlign = Paint.Align.LEFT
        }
    }

    /** Power and regen ambience: a thin dithered line, amber when drawing power, green when regenerating, red on brake. */
    private fun brakeLevel(s: HudState): Float {
        val v = s.brakeVolts ?: return if (s.brake) 1f else 0f
        if (brakeLo.isNaN() || v < brakeLo) brakeLo = v
        if (brakeHi.isNaN() || v > brakeHi) brakeHi = v
        val span = brakeHi - brakeLo
        if (span < 0.3) return if (s.brake) 1f else 0f   // not enough range seen yet
        return ((v - brakeLo) / span).toFloat().coerceIn(0f, 1f)
    }

    /**
     * The bottom ambience line, drawn with the same ordered dither as the glows: density is intensity.
     * Amber = drawing power, green = regenerating, red = brake (denser the harder the pull).
     * Reverse hatches the line so it reads differently from forward power at a glance.
     */
    private fun drawPowerLine(c: Canvas, s: HudState) {
        val d = resources.displayMetrics.density
        val amps = s.motorAmps ?: 0.0
        val brake = brakeLevel(s).let { if (s.brake) max(it, 0.35f) else it }
        val power = (abs(amps) / 80.0).coerceIn(0.0, 1.0).toFloat()
        val level = max(brake, power)
        if (level < 0.03f) return
        val color = when { brake >= power && brake > 0.03f -> fault; amps < 0 -> ok; else -> amber }
        val cell = max(2, (d * 1.5f).toInt())
        val half = (width / 2f * (0.25f + 0.75f * level)).toInt()
        val x0 = width / 2 - half
        val rows = 4
        p.style = Paint.Style.FILL
        p.color = color
        val reverse = s.mode == DriveMode.Reverse
        var x = x0
        while (x < x0 + 2 * half) {
            val edge = 1f - abs((x - width / 2f) / half).coerceAtMost(1f)   // fades out toward both ends
            val density = (0.35f + 0.65f * level) * edge.coerceAtLeast(0.15f)
            for (r in 0 until rows) {
                val t = bayer[(r % 4) * 4 + ((x / cell) % 4)] / 16f
                val hatch = reverse && ((x / cell + r) % 3 != 0)
                if (t < density && !hatch) {
                    val y = height - (rows - r) * cell.toFloat()
                    c.drawRect(x.toFloat(), y, (x + cell).toFloat(), y + cell, p)
                }
            }
            x += cell
        }
    }

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
}
