package com.melleeyyy.hasbi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.animation.LinearInterpolator
import android.animation.ValueAnimator
import kotlin.math.min
import kotlin.math.sin

object C {
    const val BG = 0xFF000000.toInt()
    const val BG2 = 0xFF030305.toInt()
    const val PANEL = 0xFF111116.toInt()
    const val TEXT = 0xFFFFFFFF.toInt()
    const val MUTED = 0xFF8B95AD.toInt()
    const val ACCENT = 0xFF0229FF.toInt()
    const val ACCENT2 = 0xFF40FF02.toInt()
    const val SURFACE = 0x0DFFFFFF
    const val SURFACE2 = 0x1AFFFFFF
    const val BORDER = 0x17FFFFFF
    const val DANGER = 0xFFFF6B81.toInt()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun gradBg(radius: Float): GradientDrawable {
    val d = GradientDrawable()
    d.setColor(C.ACCENT)
    d.cornerRadius = radius
    return d
}

fun gradBg2(radius: Float): GradientDrawable {
    val d = GradientDrawable()
    d.setColor(C.ACCENT2)
    d.cornerRadius = radius
    return d
}

fun surfaceBg(radius: Float, stroke: Boolean = true): GradientDrawable {
    val d = GradientDrawable()
    d.setColor(C.SURFACE)
    d.cornerRadius = radius
    if (stroke) d.setStroke(1, C.BORDER)
    return d
}

fun ripple(content: android.graphics.drawable.Drawable): RippleDrawable {
    val mask = android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.RectShape())
    return RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF), content, mask)
}

fun fmt(s: Double?): String {
    if (s == null || s < 0 || s.isInfinite()) return "0:00"
    val total = s.toInt()
    return "${total / 60}:${if (total % 60 < 10) "0" else ""}${total % 60}"
}

class EqView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE) { if (!anim.isRunning) anim.start() }
        else anim.cancel()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val bw = w / 5f
        paint.color = Color.WHITE
        for (i in 0..2) {
            val cx = w / 2f + (i - 1) * bw * 1.9f
            val sc = 0.35f + 0.65f * (0.5f + 0.5f * sin((phase + i * 0.33f) * 6.2832f))
            val bh = h * sc
            c.drawRoundRect(cx - bw / 2, h / 2f - bh / 2, cx + bw / 2, h / 2f + bh / 2, bw / 2, bw / 2, paint)
        }
    }
}

class DiscView(ctx: Context) : View(ctx) {
    var progress = 0f
    private var angle = 0f
    private var spinning = false
    private val density = resources.displayMetrics.density
    private val ringW = 8f * density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ringW; color = 0x1FFFFFFF
    }
    private val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ringW; color = C.ACCENT2
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1322.toInt() }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f * density; color = 0x16FFFFFF
    }
    private var shader: Unit? = null
    private val rect = RectF()
    private val spinAnim = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 16000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            angle = it.animatedValue as Float
            if (spinning) invalidate()
        }
    }

    fun setSpin(s: Boolean) {
        spinning = s
        if (s) spinAnim.start() else { spinAnim.cancel(); angle = 0f; invalidate() }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val inset = ringW / 2f + 4f * density
        rect.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(c: Canvas) {
        val w = width; val h = height
        val cx = w / 2f; val cy = h / 2f
        c.drawArc(rect, 0f, 360f, false, trackPaint)
        if (progress > 0.002f) {
            c.save()
            c.rotate(-90f, cx, cy)
            c.drawArc(rect, 0f, 360f * progress, false, progPaint)
            c.restore()
        }
        val r = min(w, h) / 2f - ringW - 6f * density
        c.drawCircle(cx, cy, r, innerPaint)
        c.drawCircle(cx, cy, r, linePaint)
        c.save()
        c.rotate(angle, cx, cy)
        val d = context.getDrawable(R.drawable.ic_music) ?: return
        val s = r * 0.6f
        d.setBounds((cx - s).toInt(), (cy - s).toInt(), (cx + s).toInt(), (cy + s).toInt())
        d.draw(c)
        c.restore()
    }
}
