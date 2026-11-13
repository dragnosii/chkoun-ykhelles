package ma.nexphasegroup.chkoun

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fond de pièces de monnaie : près du doigt, les pièces se retournent et deviennent dorées ;
 * un toucher lance une onde. Immobile (aucun calcul) quand personne ne touche l'écran.
 */
class FondPieces(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private var fx = -9999f
    private var fy = -9999f
    private var actif = false
    private var forceDoigt = 0f
    private val ondes = ArrayList<FloatArray>() // x, y, t0
    private val pas = ctx.dp(34f)
    private val rayon = ctx.dp(5f)

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    /** Toutes les 5 secondes, une onde dorée traverse les pièces depuis un point au hasard. */
    private val ambiance = object : Runnable {
        override fun run() {
            if (width > 0 && animationsOk() && !actif) {
                ondes.add(floatArrayOf(Random.nextFloat() * width, height * (0.15f + Random.nextFloat() * 0.5f), SystemClock.uptimeMillis() / 1000f))
                invalidate()
            }
            postDelayed(this, 5200)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); postDelayed(ambiance, 1800) }
    override fun onDetachedFromWindow() { removeCallbacks(ambiance); super.onDetachedFromWindow() }

    fun toucher(x: Float, y: Float, enfonce: Boolean) {
        fx = x; fy = y
        if (enfonce) ondes.add(floatArrayOf(x, y, SystemClock.uptimeMillis() / 1000f))
        actif = true
        invalidate()
    }

    fun lacher() { actif = false; invalidate() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> toucher(e.x, e.y, true)
            MotionEvent.ACTION_MOVE -> toucher(e.x, e.y, false)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> lacher()
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val t = SystemClock.uptimeMillis() / 1000f
        val anim = animationsOk()
        forceDoigt = if (actif) min(1f, forceDoigt + 0.12f) else max(0f, forceDoigt - 0.06f)
        ondes.removeAll { t - it[2] > 1.6f }
        val h = height.toFloat(); val w = width.toFloat()
        var y = pas / 2; var rang = 0
        while (y < h + pas) {
            var x = pas / 2 + if (rang % 2 == 1) pas / 2 else 0f
            while (x < w + pas) {
                val d = hypot(x - fx, y - fy)
                var k = max(0f, 1f - d / dp(150f)) * forceDoigt
                if (anim) for (o in ondes) {
                    val r = (t - o[2]) * dp(420f)
                    val dw = abs(hypot(x - o[0], y - o[1]) - r)
                    k = max(k, max(0f, 1f - dw / dp(40f)) * (1f - (t - o[2]) / 1.6f))
                }
                val fondu = (1f - y / (h * 1.05f)).coerceIn(0.15f, 1f)
                val rr = rayon * (1f + k * 0.9f)
                c.save()
                c.translate(x, y)
                // La pièce se retourne : on écrase sa largeur.
                val flip = if (k > 0.02f) abs(cos(k * PI.toFloat() * 1.5f)) .coerceAtLeast(0.12f) else 1f
                c.scale(flip, 1f)
                if (k > 0.04f) {
                    p.color = C.SAFRAN; p.alpha = (70 + k * 185).toInt().coerceAtMost(255); p.strokeWidth = dp(1.2f + k)
                } else {
                    p.color = 0xFF7874DC.toInt(); p.alpha = (75 * fondu).toInt(); p.strokeWidth = dp(1f)
                }
                c.drawCircle(0f, 0f, rr, p)
                c.drawCircle(0f, 0f, rr * 0.55f, p)
                c.restore()
                x += pas
            }
            y += pas; rang++
        }
        if (anim && (actif || forceDoigt > 0f || ondes.isNotEmpty())) postInvalidateOnAnimation()
    }

    private fun dp(v: Float) = context.dp(v)
}

/**
 * Titre « Chkoun ykhelles ? » : les lettres s'épaississent et deviennent dorées sous le doigt.
 * À l'ouverture, une vague passe une fois sur le titre.
 */
class TitreVivant(ctx: Context) : View(ctx) {
    private val lignes = listOf("Chkoun", "ykhelles ?")
    private val tailleMax = ctx.dp(64f)
    private var taille = tailleMax
    private val paints = HashMap<Int, Paint>()
    private var fx = -9999f
    private var fy = -9999f
    private var force = 0f
    private var actif = false
    private var intro: ValueAnimator? = null
    /** Largeur fixe de chaque lettre, mesurée à un poids moyen : le titre ne bouge pas quand les lettres s'épaississent. */
    private var avances: List<FloatArray> = emptyList()
    /** Relaye le doigt au fond de pièces : x, y (dans le parent) et le type de geste. */
    var surToucher: ((Float, Float, Int) -> Unit)? = null

    init { contentDescription = "Chkoun ykhelles ?" }

    private fun paint(poids: Int): Paint {
        val q = (poids / 50) * 50
        return paints.getOrPut(q) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = taille; typeface = Polices.rubik(context, q); textAlign = Paint.Align.CENTER }
        }
    }

    private fun calculer(largeur: Int) {
        if (largeur <= 0) return
        val ref = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Polices.rubik(context, 650); textSize = tailleMax }
        val plusLarge = lignes.maxOf { mot -> mot.sumOf { ref.measureText(it.toString()).toDouble() }.toFloat() * 1.04f }
        taille = if (plusLarge > largeur) tailleMax * largeur / plusLarge else tailleMax
        ref.textSize = taille
        paints.clear()
        avances = lignes.map { mot -> FloatArray(mot.length) { ref.measureText(mot[it].toString()) * 1.04f } }
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        calculer(w)
        setMeasuredDimension(w, (taille * 2.05f).toInt())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animationsOk()) return
        intro = ValueAnimator.ofFloat(-0.2f, 1.2f).apply {
            duration = 1300; startDelay = 250
            addUpdateListener {
                if (!actif) { val v = it.animatedValue as Float; fx = v * width; fy = taille * 1.0f; force = sin(v.coerceIn(0f, 1f) * PI.toFloat()) ; invalidate() }
            }
            start()
        }
    }

    override fun onDetachedFromWindow() { intro?.cancel(); super.onDetachedFromWindow() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { actif = true; intro?.cancel(); fx = e.x; fy = e.y; force = 1f }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> actif = false
        }
        surToucher?.invoke(e.x + left, e.y + top, e.actionMasked)
        invalidate()
        return true
    }

    override fun onDraw(c: Canvas) {
        if (!actif && intro?.isRunning != true && force > 0f) force = max(0f, force - 0.05f)
        if (avances.size != lignes.size) return
        for ((li, mot) in lignes.withIndex()) {
            val base = taille * (0.86f + li * 0.95f)
            val cy = base - taille * 0.35f
            var x = 0f
            for ((i, ch) in mot.withIndex()) {
                val av = avances[li][i]
                val cx = x + av / 2
                x += av
                val d = hypot(cx - fx, cy - fy)
                val k = max(0f, 1f - d / (taille * 2.6f)) * force
                val pt = paint((500 + k * 380).toInt())
                pt.color = if (k > 0.35f) C.SAFRAN else C.LAIT
                c.drawText(ch.toString(), cx, base, pt)
            }
        }
        if (!actif && force > 0f && intro?.isRunning != true) postInvalidateOnAnimation()
    }
}

/**
 * La roue : on la lance d'un geste ou avec le bouton. Le gagnant est tiré au sort à l'avance
 * de façon équitable, puis la roue ralentit pour s'arrêter sur lui. L'aiguille claque à chaque case.
 */
class Roue(ctx: Context) : View(ctx) {
    var noms: List<String> = emptyList()
        set(v) { field = v; invalidate(); contentDescription = "Roue avec ${v.size} prénoms" }
    var quandArret: ((Int) -> Unit)? = null
    var quandDepart: (() -> Unit)? = null

    private var rot = 0f
    private var bascule = 0f
    private var derniereCase = -1
    private var anim: ValueAnimator? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Polices.rubik(ctx, 700) }
    private val chemin = Path()
    private var angleTouche = 0f
    private var tempsTouche = 0L
    private var vitesse = 0f
    private var glisse = false
    /** Avant le premier lancer, la roue tourne doucement toute seule pour inviter à jouer. */
    private var auRepos = true

    val tourne get() = anim?.isRunning == true

    private fun caseSous(r: Float): Int {
        val n = noms.size; if (n == 0) return 0
        val a = 360f / n
        val x = ((360f - (r % 360f)) % 360f + 360f) % 360f
        return (x / a).toInt() % n
    }

    private fun angle(e: MotionEvent): Float = Math.toDegrees(atan2((e.y - height / 2f).toDouble(), (e.x - width / 2f).toDouble())).toFloat()

    fun lancer(force: Float = 1f) {
        if (noms.size < 2 || tourne) return
        auRepos = false
        val n = noms.size; val a = 360f / n
        val gagnant = Random.nextInt(n)
        val dansCase = a * (0.15f + Random.nextFloat() * 0.7f)
        // La case i est sous l'aiguille quand (360 - rot) mod 360 vaut i*a + dansCase.
        val cible0 = 360f - (gagnant * a + dansCase)
        val tours = 5 + (force * 3).toInt()
        var cible = cible0 + 360f * (Math.floor((rot / 360f).toDouble()).toFloat() + tours)
        while (cible < rot + 360f * 4) cible += 360f
        quandDepart?.invoke()
        if (!animationsOk()) {
            rot = cible; invalidate(); quandArret?.invoke(caseSous(rot)); return
        }
        val depart = rot
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (4200 + force * 900).toLong()
            interpolator = PathInterpolator(0.12f, 0.9f, 0.2f, 1f)
            addUpdateListener {
                rot = depart + (cible - depart) * (it.animatedValue as Float)
                val c = caseSous(rot)
                if (c != derniereCase) { derniereCase = c; bascule = -22f; performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
                bascule *= 0.82f
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { bascule = 0f; invalidate(); quandArret?.invoke(caseSous(rot)) }
            })
            start()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (tourne || noms.size < 2) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { auRepos = false; parent?.requestDisallowInterceptTouchEvent(true); angleTouche = angle(e); tempsTouche = e.eventTime; vitesse = 0f; glisse = false }
            MotionEvent.ACTION_MOVE -> {
                val a = angle(e); var d = a - angleTouche
                if (d > 180) d -= 360f; if (d < -180) d += 360f
                val dt = max(1L, e.eventTime - tempsTouche)
                vitesse = d / dt * 16.7f
                angleTouche = a; tempsTouche = e.eventTime
                if (abs(d) > 0.5f) glisse = true
                rot += d
                val c = caseSous(rot); if (c != derniereCase) { derniereCase = c; bascule = -18f * if (d >= 0) 1 else -1; performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                bascule = 0f
                if (glisse && abs(vitesse) > 2f) lancer((abs(vitesse) / 20f).coerceIn(0.3f, 1.5f)) else invalidate()
            }
        }
        return true
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        setMeasuredDimension(w, (w * 1.06f).toInt())
    }

    override fun onDraw(c: Canvas) {
        val n = noms.size
        val cx = width / 2f
        val haut = context.dp(26f)
        val r = min(width / 2f, (height - haut) / 2f) - context.dp(4f)
        val cy = haut + r
        // Bord sombre
        p.style = Paint.Style.FILL; p.color = 0xFF0E0C2B.toInt(); c.drawCircle(cx, cy, r + context.dp(3f), p)
        val ovale = RectF(cx - r, cy - r, cx + r, cy + r)
        c.save(); c.rotate(rot, cx, cy)
        if (n > 0) {
            val a = 360f / n
            for (i in 0 until n) {
                p.style = Paint.Style.FILL; p.color = C.fond(i)
                c.drawArc(ovale, -90f + i * a, a, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = context.dp(3f); p.color = 0xFF0E0C2B.toInt()
                c.drawArc(ovale, -90f + i * a, a, true, p)
            }
            for (i in 0 until n) {
                val mid = -90f + i * a + a / 2
                c.save(); c.rotate(mid, cx, cy)
                val nom = noms[i]
                t.color = C.texteSur(i)
                t.textSize = context.dp(if (n <= 6) 19f else if (n <= 9) 16f else 14f)
                val dispo = r * 0.6f
                while (t.measureText(nom) > dispo && t.textSize > context.dp(10f)) t.textSize -= context.dp(1f)
                t.textAlign = Paint.Align.RIGHT
                c.drawText(nom, cx + r * 0.9f, cy + t.textSize * 0.35f, t)
                c.restore()
            }
            // Ampoules du bord : elles clignotent en chenillard pendant que la roue tourne.
            val nbA = 24
            for (j in 0 until nbA) {
                val ang = Math.toRadians((j * 360.0 / nbA))
                val x = cx + (r - context.dp(9f)) * sin(ang).toFloat(); val y = cy - (r - context.dp(9f)) * cos(ang).toFloat()
                val allume = !tourne || ((j + (rot / 15f).toInt()) % 2 == 0)
                p.style = Paint.Style.FILL; p.color = if (allume) C.SAFRAN else 0xFF6B5420.toInt()
                c.drawCircle(x, y, context.dp(3.6f), p)
            }
        }
        c.restore()
        // Moyeu
        p.style = Paint.Style.FILL; p.color = C.NUIT; c.drawCircle(cx, cy, r * 0.22f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = context.dp(3f); p.color = C.SAFRAN; c.drawCircle(cx, cy, r * 0.22f, p)
        p.style = Paint.Style.FILL; p.color = C.MENTHE; c.drawCircle(cx, cy, r * 0.06f, p)
        // Aiguille en forme de verre de thé
        c.save(); c.rotate(bascule, cx, haut - context.dp(18f))
        val gw = context.dp(30f); val gt = haut - context.dp(22f); val gb = haut + context.dp(14f)
        chemin.reset()
        chemin.moveTo(cx - gw / 2, gt); chemin.lineTo(cx + gw / 2, gt); chemin.lineTo(cx + gw * 0.36f, gb); chemin.lineTo(cx - gw * 0.36f, gb); chemin.close()
        p.style = Paint.Style.FILL; p.color = C.LAIT; c.drawPath(chemin, p)
        chemin.reset()
        val lt = gt + (gb - gt) * 0.38f
        chemin.moveTo(cx - gw * 0.44f, lt); chemin.lineTo(cx + gw * 0.44f, lt); chemin.lineTo(cx + gw * 0.33f, gb - context.dp(2f)); chemin.lineTo(cx - gw * 0.33f, gb - context.dp(2f)); chemin.close()
        p.color = C.MENTHE; c.drawPath(chemin, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = context.dp(4f); p.strokeCap = Paint.Cap.ROUND; p.color = C.SAFRAN
        c.drawLine(cx, gb + context.dp(2f), cx, gb + context.dp(12f), p)
        c.restore()
        if (auRepos && n >= 2 && animationsOk() && isShown) { rot += 0.18f; postInvalidateOnAnimation() }
    }

    override fun onDetachedFromWindow() { anim?.cancel(); super.onDetachedFromWindow() }
}

/** Verre de thé du duel : rouge (trop chaud) puis vert menthe (touche !). */
class VerreThe(ctx: Context) : View(ctx) {
    var couleurThe = C.GRENADE
        set(v) { field = v; invalidate() }
    var anneau = C.SAFRAN
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chemin = Path()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val cx = w / 2; val cy = h / 2; val r = min(w, h) / 2 - context.dp(4f)
        p.style = Paint.Style.FILL; p.color = C.NUIT; c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = context.dp(4f); p.color = anneau; c.drawCircle(cx, cy, r, p)
        val gw = r * 0.95f; val top = cy - r * 0.5f; val bas = cy + r * 0.62f
        chemin.reset(); chemin.moveTo(cx - gw / 2, top); chemin.lineTo(cx + gw / 2, top); chemin.lineTo(cx + gw * 0.36f, bas); chemin.lineTo(cx - gw * 0.36f, bas); chemin.close()
        p.style = Paint.Style.FILL; p.color = 0x26F7F0E4; c.drawPath(chemin, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = context.dp(2.4f); p.color = C.LAIT; c.drawPath(chemin, p)
        val lt = top + (bas - top) * 0.3f
        chemin.reset(); chemin.moveTo(cx - gw * 0.45f, lt); chemin.lineTo(cx + gw * 0.45f, lt); chemin.lineTo(cx + gw * 0.34f, bas - context.dp(3f)); chemin.lineTo(cx - gw * 0.34f, bas - context.dp(3f)); chemin.close()
        p.style = Paint.Style.FILL; p.color = couleurThe; c.drawPath(chemin, p)
    }
}

/** Disposition en lignes qui reviennent à la ligne (pour les prénoms). */
class Rangees(ctx: Context) : ViewGroup(ctx) {
    private val ecart = ctx.dpi(10)

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val largeur = MeasureSpec.getSize(wSpec) - paddingLeft - paddingRight
        var x = 0; var y = 0; var hLigne = 0
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            measureChild(v, MeasureSpec.makeMeasureSpec(largeur, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            if (x > 0 && x + v.measuredWidth > largeur) { x = 0; y += hLigne + ecart; hLigne = 0 }
            x += v.measuredWidth + ecart
            hLigne = max(hLigne, v.measuredHeight)
        }
        setMeasuredDimension(MeasureSpec.getSize(wSpec), y + hLigne + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val largeur = r - l - paddingLeft - paddingRight
        var x = 0; var y = 0; var hLigne = 0
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (x > 0 && x + v.measuredWidth > largeur) { x = 0; y += hLigne + ecart; hLigne = 0 }
            v.layout(paddingLeft + x, paddingTop + y, paddingLeft + x + v.measuredWidth, paddingTop + y + v.measuredHeight)
            x += v.measuredWidth + ecart
            hLigne = max(hLigne, v.measuredHeight)
        }
    }
}

/** Pluie de pièces de monnaie (2,5 secondes) quand le verdict tombe. */
class PluieDePieces(ctx: Context) : View(ctx) {
    private class Piece(var x: Float, var y: Float, val vy: Float, val vx: Float, val r: Float, val couleur: Int, val tour: Float, val phase: Float)
    private val pieces = ArrayList<Piece>()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var debut = 0L

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun lancer() {
        if (!animationsOk() || width == 0) return
        pieces.clear()
        val cols = intArrayOf(C.SAFRAN, C.SAFRAN, C.MENTHE, C.MAJORELLE, C.GRENADE, C.LAIT)
        repeat(46) {
            pieces.add(Piece(Random.nextFloat() * width, -context.dp(40f) - Random.nextFloat() * height * 0.6f,
                context.dp(5f + Random.nextFloat() * 6f), context.dp(Random.nextFloat() * 2f - 1f), context.dp(6f + Random.nextFloat() * 6f),
                cols[Random.nextInt(cols.size)], 4f + Random.nextFloat() * 8f, Random.nextFloat() * 6f))
        }
        debut = SystemClock.uptimeMillis()
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        if (pieces.isEmpty()) return
        val t = (SystemClock.uptimeMillis() - debut) / 1000f
        val fondu = (1f - (t - 1.8f) / 0.8f).coerceIn(0f, 1f)
        for (q in pieces) {
            q.y += q.vy; q.x += q.vx
            val flip = abs(cos(q.phase + t * q.tour))
            c.save(); c.translate(q.x, q.y); c.scale(max(0.15f, flip), 1f)
            p.style = Paint.Style.FILL; p.color = q.couleur; p.alpha = (255 * fondu).toInt()
            c.drawCircle(0f, 0f, q.r, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = context.dp(1.5f); p.color = C.NUIT; p.alpha = (110 * fondu).toInt()
            c.drawCircle(0f, 0f, q.r * 0.6f, p)
            c.restore()
        }
        if (t < 2.6f) postInvalidateOnAnimation() else pieces.clear()
    }
}

/** Barre de proportion payé / épargné. Elle se remplit à l'ouverture. */
class BarreBilan(ctx: Context) : View(ctx) {
    var paye = 0.0
    var epargne = 0.0
    var progres = 1f
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onMeasure(wSpec: Int, hSpec: Int) = setMeasuredDimension(MeasureSpec.getSize(wSpec), context.dpi(14))

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val rr = h / 2
        p.color = C.BORD; c.drawRoundRect(0f, 0f, w, h, rr, rr, p)
        val tot = paye + epargne
        if (tot <= 0) return
        c.save(); c.clipRect(0f, 0f, w * progres, h)
        val xp = (w * (paye / tot)).toFloat()
        val gap = context.dp(3f)
        if (paye > 0) { p.color = C.GRENADE; c.drawRoundRect(0f, 0f, max(h, xp - gap / 2), h, rr, rr, p) }
        if (epargne > 0) { p.color = C.MENTHE; c.drawRoundRect(min(w - h, xp + gap / 2), 0f, w, h, rr, rr, p) }
        c.restore()
    }
}
