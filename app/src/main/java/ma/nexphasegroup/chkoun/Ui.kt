package ma.nexphasegroup.chkoun

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

/** Couleurs de l'appli (« Terrasse de minuit »). */
object C {
    const val NUIT = 0xFF15123A.toInt()
    const val NUIT2 = 0xFF1E1A4E.toInt()
    const val BORD = 0xFF2E2970.toInt()
    const val BORD2 = 0xFF3A3485.toInt()
    const val MAJORELLE = 0xFF5B5CFF.toInt()
    const val SAFRAN = 0xFFFFB938.toInt()
    const val MENTHE = 0xFF6FE3B4.toInt()
    const val GRENADE = 0xFFFF5C74.toInt()
    const val LAIT = 0xFFF7F0E4.toInt()
    const val MUET = 0xFFC9C3EC.toInt()
    const val DISCRET = 0xFFA9A3D6.toInt()
    const val LILAS = 0xFF9C8CFF.toInt()
    const val ERREUR = 0xFFFF8A9A.toInt()
    const val BLANC = 0xFFFFFFFF.toInt()

    val PALETTE = intArrayOf(MAJORELLE, SAFRAN, MENTHE, GRENADE, LAIT, LILAS)
    fun fond(i: Int) = PALETTE[i % PALETTE.size]
    fun texteSur(i: Int) = if (i % PALETTE.size == 0) BLANC else NUIT
}

/** Polices : Rubik (variable, poids 300 à 900) et Lalezar (arabe et latin). */
object Polices {
    private val cache = HashMap<Int, Typeface>()
    private var lalezarTf: Typeface? = null

    fun rubik(ctx: Context, poids: Int): Typeface {
        val p = poids.coerceIn(300, 900)
        return cache.getOrPut(p) {
            try {
                Typeface.Builder(ctx.assets, "fonts/rubik.ttf").setFontVariationSettings("'wght' $p").build()
            } catch (e: Exception) {
                Typeface.create(Typeface.SANS_SERIF, if (p >= 600) Typeface.BOLD else Typeface.NORMAL)
            }
        }
    }

    fun lalezar(ctx: Context): Typeface {
        lalezarTf?.let { return it }
        val tf = try { Typeface.createFromAsset(ctx.assets, "fonts/lalezar.ttf") } catch (e: Exception) { Typeface.DEFAULT_BOLD }
        lalezarTf = tf
        return tf
    }
}

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

/** Faux si l'utilisateur a coupé les animations dans les réglages d'Android. */
fun animationsOk(): Boolean = ValueAnimator.areAnimatorsEnabled()

fun forme(couleur: Int, rayon: Float, bord: Int = 0, epaisseur: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(couleur)
        cornerRadius = rayon
        if (epaisseur > 0) setStroke(epaisseur, bord)
    }

fun formeCoins(couleur: Int, hg: Float, hd: Float, bd: Float, bg: Float, bord: Int = 0, epaisseur: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(couleur)
        cornerRadii = floatArrayOf(hg, hg, hd, hd, bd, bd, bg, bg)
        if (epaisseur > 0) setStroke(epaisseur, bord)
    }

fun avecOnde(fond: Drawable, onde: Int = 0x33FFFFFF): RippleDrawable = RippleDrawable(ColorStateList.valueOf(onde), fond, null)

fun texte(ctx: Context, s: CharSequence, tailleSp: Float, poids: Int = 400, couleur: Int = C.LAIT): TextView =
    TextView(ctx).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tailleSp)
        typeface = Polices.rubik(ctx, poids)
        setTextColor(couleur)
        setLineSpacing(0f, 1.2f)
    }

fun arabe(ctx: Context, tailleSp: Float, couleur: Int = C.SAFRAN): TextView =
    TextView(ctx).apply {
        text = "شكون يخلّص؟"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tailleSp)
        typeface = Polices.lalezar(ctx)
        setTextColor(couleur)
        textDirection = View.TEXT_DIRECTION_RTL
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        includeFontPadding = false
    }

/** Gros bouton en pilule. */
fun bouton(ctx: Context, libelle: String, fond: Int, encre: Int, hauteurDp: Int = 60, action: () -> Unit): Button =
    Button(ctx).apply {
        text = libelle
        isAllCaps = false
        stateListAnimator = null
        setTextColor(encre)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        typeface = Polices.rubik(ctx, 800)
        background = avecOnde(forme(fond, ctx.dp(hauteurDp / 2f)), if (fond == C.SAFRAN || fond == C.LAIT || fond == C.MENTHE) 0x3315123A else 0x33FFFFFF)
        minHeight = ctx.dpi(hauteurDp)
        minimumHeight = ctx.dpi(hauteurDp)
        setPadding(ctx.dpi(20), 0, ctx.dpi(20), 0)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dpi(hauteurDp))
        setOnClickListener { action() }
    }

/** Bouton contour (action secondaire). */
fun boutonContour(ctx: Context, libelle: String, hauteurDp: Int = 52, action: () -> Unit): Button =
    Button(ctx).apply {
        text = libelle
        isAllCaps = false
        stateListAnimator = null
        setTextColor(C.LAIT)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        typeface = Polices.rubik(ctx, 600)
        background = avecOnde(forme(0, ctx.dp(hauteurDp / 2f), C.BORD2, ctx.dpi(2)))
        minHeight = ctx.dpi(hauteurDp)
        minimumHeight = ctx.dpi(hauteurDp)
        setPadding(ctx.dpi(16), 0, ctx.dpi(16), 0)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dpi(hauteurDp))
        setOnClickListener { action() }
    }

/** Bouton rond avec une icône dessinée (48 dp, avec description pour les lecteurs d'écran). */
fun boutonIcone(ctx: Context, type: Icone.Type, description: String, fond: Int = 0x14F7F0E4, couleur: Int = C.LAIT, action: () -> Unit): ImageButton =
    ImageButton(ctx).apply {
        setImageDrawable(Icone(type, couleur, ctx.dp(1f)))
        contentDescription = description
        background = avecOnde(forme(fond, ctx.dp(24f)))
        scaleType = android.widget.ImageView.ScaleType.CENTER
        layoutParams = LinearLayout.LayoutParams(ctx.dpi(48), ctx.dpi(48))
        setOnClickListener { action() }
    }

fun colonne(ctx: Context): LinearLayout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
fun ligne(ctx: Context): LinearLayout = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
fun ressort(ctx: Context): View = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(1, 0, 1f) }
fun espace(ctx: Context, dp: Int): View = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(1, ctx.dpi(dp)) }
fun espaceH(ctx: Context, dp: Int): View = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(ctx.dpi(dp), 1) }

fun LinearLayout.ajoute(v: View, hautDp: Int = 0): View {
    val lp = (v.layoutParams as? LinearLayout.LayoutParams)
        ?: LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    if (hautDp > 0) lp.topMargin = context.dpi(hautDp)
    addView(v, lp)
    return v
}

fun pleineLargeur() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

/** Icônes au trait, dessinées en code (pas d'emoji, pas de fichier). */
class Icone(private val type: Type, private val couleur: Int, private val unite: Float) : Drawable() {
    enum class Type { RETOUR, FERMER, HISTORIQUE, PARTAGER, CORBEILLE, PLUS, MOINS, CRAYON, SUIVANT, ECLAIR, TELEPHONE }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = couleur
        strokeWidth = 2f
    }
    private val chemin = Path()

    override fun getIntrinsicWidth() = (24 * unite).toInt()
    override fun getIntrinsicHeight() = (24 * unite).toInt()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()) / 24f
        canvas.save()
        canvas.translate(b.left + (b.width() - 24 * s) / 2, b.top + (b.height() - 24 * s) / 2)
        canvas.scale(s, s)
        chemin.reset()
        when (type) {
            Type.RETOUR -> { chemin.moveTo(15f, 18f); chemin.lineTo(9f, 12f); chemin.lineTo(15f, 6f) }
            Type.SUIVANT -> { chemin.moveTo(9f, 18f); chemin.lineTo(15f, 12f); chemin.lineTo(9f, 6f) }
            Type.FERMER -> { chemin.moveTo(6f, 6f); chemin.lineTo(18f, 18f); chemin.moveTo(18f, 6f); chemin.lineTo(6f, 18f) }
            Type.PLUS -> { chemin.moveTo(12f, 5f); chemin.lineTo(12f, 19f); chemin.moveTo(5f, 12f); chemin.lineTo(19f, 12f) }
            Type.MOINS -> { chemin.moveTo(5f, 12f); chemin.lineTo(19f, 12f) }
            Type.HISTORIQUE -> {
                chemin.addArc(RectF(3f, 3f, 21f, 21f), 200f, 290f)
                chemin.moveTo(3.5f, 4f); chemin.lineTo(3.5f, 9f); chemin.lineTo(8.5f, 9f)
                chemin.moveTo(12f, 8f); chemin.lineTo(12f, 12f); chemin.lineTo(15f, 14f)
            }
            Type.PARTAGER -> {
                chemin.moveTo(4f, 12f); chemin.lineTo(4f, 19f); chemin.quadTo(4f, 21f, 6f, 21f); chemin.lineTo(18f, 21f)
                chemin.quadTo(20f, 21f, 20f, 19f); chemin.lineTo(20f, 12f)
                chemin.moveTo(16f, 6f); chemin.lineTo(12f, 2f); chemin.lineTo(8f, 6f); chemin.moveTo(12f, 2f); chemin.lineTo(12f, 15f)
            }
            Type.CORBEILLE -> {
                chemin.moveTo(4f, 7f); chemin.lineTo(20f, 7f)
                chemin.moveTo(6f, 7f); chemin.lineTo(7f, 20f); chemin.lineTo(17f, 20f); chemin.lineTo(18f, 7f)
                chemin.moveTo(9f, 7f); chemin.lineTo(9f, 4f); chemin.lineTo(15f, 4f); chemin.lineTo(15f, 7f)
            }
            Type.ECLAIR -> {
                chemin.moveTo(13f, 2f); chemin.lineTo(4f, 14f); chemin.lineTo(11f, 14f); chemin.lineTo(10f, 22f); chemin.lineTo(19f, 10f); chemin.lineTo(12f, 10f); chemin.close()
            }
            Type.TELEPHONE -> {
                chemin.addRoundRect(RectF(8f, 3f, 16f, 21f), 2f, 2f, Path.Direction.CW)
                chemin.moveTo(4f, 8f); chemin.lineTo(4f, 16f); chemin.moveTo(20f, 8f); chemin.lineTo(20f, 16f)
                chemin.moveTo(11f, 18f); chemin.lineTo(13f, 18f)
            }
            Type.CRAYON -> {
                chemin.moveTo(4f, 20f); chemin.lineTo(8f, 19f); chemin.lineTo(19f, 8f); chemin.lineTo(16f, 5f); chemin.lineTo(5f, 16f); chemin.close()
            }
        }
        canvas.drawPath(chemin, p)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { p.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Petite roue colorée (icône du bouton « Laisser le sort décider »). */
class MiniRoue(private val unite: Float) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun getIntrinsicWidth() = (34 * unite).toInt()
    override fun getIntrinsicHeight() = (34 * unite).toInt()
    override fun draw(canvas: Canvas) {
        val b = bounds
        val cx = b.exactCenterX(); val cy = b.exactCenterY(); val r = minOf(b.width(), b.height()) / 2f
        val o = RectF(cx - r, cy - r, cx + r, cy + r)
        for (i in 0 until 5) {
            p.style = Paint.Style.FILL; p.color = C.fond(i)
            canvas.drawArc(o, -90f + i * 72f, 72f, true, p)
        }
        p.color = C.NUIT; canvas.drawCircle(cx, cy, r * 0.36f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.1f; p.color = C.SAFRAN; canvas.drawCircle(cx, cy, r * 0.36f, p)
    }
    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { p.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
