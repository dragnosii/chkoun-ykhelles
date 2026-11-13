package ma.nexphasegroup.chkoun

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.view.View
import java.io.File
import java.io.FileOutputStream

/** Ce qu'affiche le ticket du verdict. */
data class Verdict(val payeur: String, val nb: Int, val mode: String, val montant: Double, val date: Long)

/** Dessine le ticket (à l'écran et dans l'image partagée). Unité = largeur / 300. */
object DessinTicket {
    fun hauteurPour(largeur: Float) = largeur * 1.42f

    fun dessiner(ctx: Context, c: Canvas, x: Float, y: Float, largeur: Float, v: Verdict) {
        val u = largeur / 300f
        val h = hauteurPour(largeur) - 14 * u
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // Corps du ticket avec bord en dents de scie en bas
        val chemin = Path()
        val r = 22 * u
        chemin.moveTo(x, y + r); chemin.quadTo(x, y, x + r, y); chemin.lineTo(x + largeur - r, y); chemin.quadTo(x + largeur, y, x + largeur, y + r)
        chemin.lineTo(x + largeur, y + h)
        var xx = x + largeur; var bas = true
        while (xx > x) { xx -= 10 * u; chemin.lineTo(maxOf(x, xx), if (bas) y + h + 12 * u else y + h); bas = !bas }
        chemin.close()
        p.color = C.LAIT; c.drawPath(chemin, p)

        val cx = x + largeur / 2
        fun txt(s: String, ty: Float, taille: Float, poids: Int, couleur: Int, police: android.graphics.Typeface? = null): Paint {
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = taille * u; typeface = police ?: Polices.rubik(ctx, poids); color = couleur; textAlign = Paint.Align.CENTER }
            c.drawText(s, cx, y + ty * u, t); return t
        }
        txt("شكون يخلّص؟", 52f, 30f, 400, C.MAJORELLE, Polices.lalezar(ctx))
        txt("Verdict du " + Format.date(v.date) + ", " + Format.heure(v.date), 76f, 13f, 400, 0xFF4A4570.toInt())
        val tir = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCFC6B3.toInt(); strokeWidth = 2 * u; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(6 * u, 5 * u), 0f) }
        c.drawLine(x + 22 * u, y + 94 * u, x + largeur - 22 * u, y + 94 * u, tir)
        txt("Ce soir, c'est", 128f, 17f, 500, C.NUIT)
        // Prénom en très gros, réduit s'il est long
        val nom = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Polices.rubik(ctx, 900); color = C.NUIT; textAlign = Paint.Align.CENTER; textSize = 64 * u }
        while (nom.measureText(v.payeur) > largeur - 40 * u && nom.textSize > 26 * u) nom.textSize -= 2 * u
        c.drawText(v.payeur, cx, y + 190 * u, nom)
        txt("qui régale toute la table", 220f, 17f, 500, C.NUIT)

        val gris = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14 * u; typeface = Polices.rubik(ctx, 400); color = 0xFF4A4570.toInt() }
        val gras = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14 * u; typeface = Polices.rubik(ctx, 700); color = C.NUIT; textAlign = Paint.Align.RIGHT }
        val lignes = listOf(
            "Amis à table" to v.nb.toString(),
            "Désigné par" to v.mode,
            "Addition" to if (v.montant > 0) Format.dhComplet(v.montant) else "non renseignée",
        )
        for ((i, l) in lignes.withIndex()) {
            val ly = y + (262 + i * 24) * u
            c.drawText(l.first, x + 24 * u, ly, gris)
            c.drawText(l.second, x + largeur - 24 * u, ly, gras)
        }
        // Quatre losanges de couleur
        val tl = 7 * u
        for ((i, col) in intArrayOf(C.MAJORELLE, C.SAFRAN, C.MENTHE, C.GRENADE).withIndex()) {
            val lx = cx + (i - 1.5f) * 20 * u; val ly = y + 362 * u
            val d = Path(); d.moveTo(lx, ly - tl); d.lineTo(lx + tl, ly); d.lineTo(lx, ly + tl); d.lineTo(lx - tl, ly); d.close()
            p.color = col; c.drawPath(d, p)
        }
        txt("Chkoun Ykhelles, une appli NexPhase Group", 398f, 12f, 400, 0xFF4A4570.toInt())
    }

    /**
     * Décor de nuit : fond bleu nuit, halos Majorelle, safran et menthe, grille de pièces qui s'efface vers le bas.
     * `u` = taille d'un « dp » dans ce dessin.
     */
    fun decor(c: Canvas, w: Float, h: Float, u: Float) {
        c.drawColor(C.NUIT)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun halo(x: Float, y: Float, r: Float, couleur: Int) {
            p.shader = android.graphics.RadialGradient(x, y, r, intArrayOf(couleur, couleur and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, p)
        }
        halo(w * 0.15f, h * 0.12f, w * 0.85f, 0x805B5CFF.toInt())
        halo(w * 0.95f, h * 0.62f, w * 0.75f, 0x55FFB938)
        halo(w * 0.1f, h * 0.9f, w * 0.6f, 0x406FE3B4)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = u
        val pas = 34 * u
        var y = pas / 2; var rang = 0
        while (y < h) {
            var x = pas / 2 + if (rang % 2 == 1) pas / 2 else 0f
            val a = ((1f - y / h) * 70).toInt().coerceIn(10, 70)
            p.color = 0xFF7874DC.toInt(); p.alpha = a
            while (x < w + pas) {
                c.drawCircle(x, y, 5 * u, p); c.drawCircle(x, y, 2.8f * u, p)
                x += pas
            }
            y += pas; rang++
        }
        val voile = Paint().apply { shader = LinearGradient(0f, h * 0.55f, 0f, h, intArrayOf(0x0015123A, 0xE615123A.toInt()), null, Shader.TileMode.CLAMP) }
        c.drawRect(0f, h * 0.55f, w, h, voile)
    }

    /** Image 1080 x 1920 au format story, prête à partager. */
    fun image(ctx: Context, v: Verdict): Bitmap {
        val w = 1080; val h = 1920
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        decor(c, w.toFloat(), h.toFloat(), 3f)
        val lt = 820f
        c.save(); c.rotate(-3f, w / 2f, 760f)
        val ombre = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000; maskFilter = android.graphics.BlurMaskFilter(60f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
        c.drawRoundRect((w - lt) / 2 + 10, 260f, (w + lt) / 2 + 10, 260f + DessinTicket.hauteurPour(lt) + 40, 60f, 60f, ombre)
        dessiner(ctx, c, (w - lt) / 2, 230f, lt, v)
        c.restore()
        val pied = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.LAIT; textSize = 44f; typeface = Polices.rubik(ctx, 600); textAlign = Paint.Align.CENTER }
        c.drawText("Et chez vous, chkoun ykhelles ?", w / 2f, 1700f, pied)
        pied.textSize = 34f; pied.color = C.MUET; pied.typeface = Polices.rubik(ctx, 400)
        c.drawText("Appli gratuite sur Google Play", w / 2f, 1760f, pied)
        return bmp
    }

    /** Enregistre l'image dans le cache et ouvre le menu de partage d'Android. */
    fun partager(ctx: Context, v: Verdict) {
        val dossier = File(ctx.cacheDir, "partage").apply { mkdirs() }
        val f = File(dossier, PartageProvider.FICHIER)
        val bmp = image(ctx, v)
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = Uri.parse("content://${PartageProvider.AUTORITE}/${PartageProvider.FICHIER}")
        val envoi = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Ce soir, c'est ${v.payeur} qui régale ! (Chkoun Ykhelles)")
            // Le ClipData fait suivre le droit de lecture jusqu'au menu de partage et à l'appli choisie.
            clipData = android.content.ClipData.newRawUri("verdict", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(envoi, "Partager le verdict").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}

/** Fond du verdict : le même décor que l'image partagée. */
class VueDecor(ctx: Context) : View(ctx) {
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(c: Canvas) = DessinTicket.decor(c, width.toFloat(), height.toFloat(), context.dp(1f))
}

/** Vue qui affiche le ticket, légèrement penché. */
class VueTicket(ctx: Context) : View(ctx) {
    var verdict: Verdict? = null
        set(v) { field = v; contentDescription = v?.let { "Verdict : ce soir, c'est ${it.payeur} qui régale" }; invalidate() }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        setMeasuredDimension(w, (DessinTicket.hauteurPour(w * 0.8f) + context.dp(30f)).toInt())
    }

    override fun onDraw(c: Canvas) {
        val v = verdict ?: return
        val lt = width * 0.8f
        c.save(); c.rotate(-3f, width / 2f, height / 2f)
        DessinTicket.dessiner(context, c, (width - lt) / 2, context.dp(12f), lt, v)
        c.restore()
    }
}

/** Donne aux autres applis l'accès en lecture à UN seul fichier : l'image du verdict. */
class PartageProvider : ContentProvider() {
    companion object {
        const val AUTORITE = "ma.nexphasegroup.chkoun.partage"
        const val FICHIER = "verdict.png"
    }

    private fun fichier(uri: Uri): File? {
        val ctx = context ?: return null
        if (uri.lastPathSegment != FICHIER) return null
        return File(File(ctx.cacheDir, "partage"), FICHIER).takeIf { it.exists() }
    }

    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/png"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") throw SecurityException("Lecture seule")
        val f = fichier(uri) ?: throw java.io.FileNotFoundException()
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val f = fichier(uri) ?: return null
        val cur = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        cur.addRow(arrayOf<Any>(FICHIER, f.length()))
        return cur
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
