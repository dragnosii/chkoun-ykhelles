package ma.nexphasegroup.chkoun

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.sqrt
import kotlin.random.Random

// ---------- Éléments communs ----------

private fun barreHaut(a: MainActivity, titre: String?, retour: Boolean = true, action: (() -> Unit)? = null): LinearLayout {
    val l = ligne(a)
    if (retour) l.addView(boutonIcone(a, Icone.Type.RETOUR, "Retour") { action?.invoke() ?: a.retour() })
    if (titre != null) {
        val t = texte(a, titre, 26f, 800)
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); lp.marginStart = a.dpi(12)
        l.addView(t, lp)
    }
    return l
}

private fun defilant(a: MainActivity, contenu: LinearLayout): ScrollView = ScrollView(a).apply {
    isFillViewport = true
    isVerticalScrollBarEnabled = false
    addView(contenu, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
}

private fun page(a: MainActivity): LinearLayout = colonne(a).apply { setPadding(a.dpi(22), a.dpi(16), a.dpi(22), a.dpi(22)) }

private fun champ(a: MainActivity, indice: String, nombre: Boolean = false): EditText = EditText(a).apply {
    id = View.generateViewId()
    hint = indice
    setHintTextColor(C.DISCRET)
    setTextColor(C.LAIT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
    typeface = Polices.rubik(a, 500)
    background = forme(C.NUIT2, a.dp(16f), C.BORD2, a.dpi(2))
    setPadding(a.dpi(18), 0, a.dpi(18), 0)
    minHeight = a.dpi(56)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, a.dpi(56))
    isSingleLine = true
    if (nombre) {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        keyListener = DigitsKeyListener.getInstance("0123456789,. ")
    } else {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }
}

private fun etiquette(a: MainActivity, s: String, pour: View? = null): TextView = texte(a, s, 14f, 500, C.MUET).apply { if (pour != null) labelFor = pour.id }

private fun auChange(e: EditText, f: (String) -> Unit) = e.addTextChangedListener(object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
    override fun afterTextChanged(s: Editable?) { f(s?.toString() ?: "") }
})

/** Fait annoncer une vue comme un bouton radio par les lecteurs d'écran. */
private fun commeRadio(v: View, coche: () -> Boolean) {
    v.accessibilityDelegate = object : View.AccessibilityDelegate() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.className = RadioButton::class.java.name
            info.isCheckable = true
            info.isChecked = coche()
        }
    }
}

private fun dialogue(a: MainActivity) = AlertDialog.Builder(a, android.R.style.Theme_Material_Dialog_Alert)

/** Le groupe de ce soir : moi en premier, puis les amis enregistrés. */
fun groupeComplet(a: MainActivity): List<String> {
    val moi = a.store.moi
    val autres = a.store.groupe.filter { !it.equals(moi, ignoreCase = true) }
    return if (moi.isEmpty()) autres else listOf(moi) + autres
}

// ---------- 1. Accueil ----------

class Accueil : Ecran() {
    private var derniere: TextView? = null

    override fun creer(a: MainActivity): View {
        val cadre = FrameLayout(a)
        val fond = FondPieces(a)
        cadre.addView(fond, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val voile = View(a).apply {
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(C.NUIT, C.NUIT, 0x0015123A))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        cadre.addView(voile, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, a.dpi(420), Gravity.BOTTOM))

        val col = colonne(a).apply { setPadding(a.dpi(26), a.dpi(14), a.dpi(26), a.dpi(20)); clipChildren = false; clipToPadding = false }
        cadre.clipChildren = false
        val haut = ligne(a)
        haut.addView(arabe(a, 30f))
        haut.addView(ressort(a).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        haut.addView(boutonIcone(a, Icone.Type.HISTORIQUE, "Mon bilan et l'historique des soirées") { a.aller(BilanEcran()) })
        col.ajoute(haut)

        val titre = TitreVivant(a)
        titre.surToucher = { x, y, geste ->
            when (geste) {
                MotionEvent.ACTION_DOWN -> fond.toucher(x + col.left, y + col.top, true)
                MotionEvent.ACTION_MOVE -> fond.toucher(x + col.left, y + col.top, false)
                else -> fond.lacher()
            }
        }
        col.ajoute(titre, 40)
        col.ajoute(texte(a, "L'addition arrive. Personne ne bouge. Laisse le sort trancher, ou partage au dirham près.", 17f, 400, C.MUET).apply { maxWidth = a.dpi(320) }, 14)
        col.addView(ressort(a))

        // Bouton principal : laisser le sort décider
        val sort = ligne(a).apply {
            background = avecOnde(formeCoins(C.MAJORELLE, a.dp(30f), a.dp(30f), a.dp(30f), a.dp(8f)))
            setPadding(a.dpi(20), a.dpi(18), a.dpi(18), a.dpi(18))
            isClickable = true; isFocusable = true
            setOnClickListener { a.aller(AmisEcran()) }
        }
        val pastille = FrameLayout(a).apply { background = forme(C.NUIT, a.dp(28f)) }
        val icone = ImageView(a).apply { setImageDrawable(MiniRoue(a.dp(1f))); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        pastille.addView(icone, FrameLayout.LayoutParams(a.dpi(34), a.dpi(34), Gravity.CENTER))
        if (animationsOk()) tourIcone = android.animation.ObjectAnimator.ofFloat(icone, "rotation", 0f, 360f).apply {
            duration = 7000; repeatCount = android.animation.ValueAnimator.INFINITE; interpolator = android.view.animation.LinearInterpolator()
        }
        sort.addView(pastille, LinearLayout.LayoutParams(a.dpi(56), a.dpi(56)))
        val ts = colonne(a)
        ts.addView(texte(a, "Laisser le sort décider", 21f, 700, C.BLANC))
        ts.addView(texte(a, "Roue, duel de réflexes ou téléphone secoué", 14f, 400, 0xFFE6E4FF.toInt()))
        sort.addView(ts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = a.dpi(16) })
        col.ajoute(sort)
        if (animationsOk()) sort.post {
            sort.animate().scaleX(1.02f).scaleY(1.02f).setDuration(260).withEndAction { sort.animate().scaleX(1f).scaleY(1f).setDuration(260).start() }.setStartDelay(1500).start()
        }

        // Bouton secondaire : partager l'addition
        val add = ligne(a).apply {
            background = avecOnde(formeCoins(0x9915123A.toInt(), a.dp(8f), a.dp(30f), a.dp(30f), a.dp(30f), C.SAFRAN, a.dpi(2)))
            setPadding(a.dpi(20), a.dpi(16), a.dpi(18), a.dpi(16))
            isClickable = true; isFocusable = true
            setOnClickListener { a.aller(AdditionEcran(maxOf(2, groupeComplet(a).size), 0.0)) }
        }
        add.addView(TextView(a).apply {
            text = "DH"; gravity = Gravity.CENTER; typeface = Polices.lalezar(a); setTextColor(C.NUIT); setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            background = forme(C.SAFRAN, a.dp(26f)); setPadding(0, a.dpi(4), 0, 0); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(a.dpi(52), a.dpi(52)))
        val ta = colonne(a)
        ta.addView(texte(a, "Partager l'addition", 20f, 700))
        ta.addView(texte(a, "Chacun sa part, pourboire compris", 14f, 400, C.MUET))
        add.addView(ta, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = a.dpi(16) })
        col.ajoute(add, 12)

        val der = texte(a, "", 14f, 400, C.MUET)
        derniere = der
        col.ajoute(der, 20)
        col.ajoute(texte(a, "Une appli NexPhase Group", 12f, 400, C.DISCRET).apply { gravity = Gravity.CENTER }, 14)

        cadre.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        // Entrée en cascade, une seule fois à l'ouverture.
        if (animationsOk()) for (i in 0 until col.childCount) {
            val v = col.getChildAt(i)
            v.alpha = 0f; v.translationY = a.dp(26f)
            v.animate().alpha(1f).translationY(0f).setStartDelay(120L + i * 70L).setDuration(520)
                .setInterpolator(android.view.animation.DecelerateInterpolator(2f)).start()
        }
        return cadre
    }

    private var tourIcone: android.animation.ObjectAnimator? = null

    override fun cache(a: MainActivity) { tourIcone?.pause() }

    override fun visible(a: MainActivity) {
        tourIcone?.let { if (it.isPaused) it.resume() else if (!it.isStarted) it.start() }
        val s = a.store.soirees().firstOrNull()
        derniere?.text = when {
            s == null -> "Aucune soirée enregistrée pour l'instant."
            s.type == "partage" -> "Dernière soirée : addition partagée à ${s.nb}" + if (s.montant > 0) ", ${Format.dhComplet(s.montant)}" else ""
            else -> "Dernière soirée : ${if (s.moiPayeur) "tu as régalé" else "${s.payeur} a régalé"}" + if (s.montant > 0) ", ${Format.dhComplet(s.montant)}" else ""
        }
    }
}

// ---------- 2. Qui est à table ? ----------

class AmisEcran : Ecran() {
    private lateinit var rangees: Rangees
    private lateinit var compte: TextView
    private lateinit var erreur: TextView
    private lateinit var cta: android.widget.Button
    private val modes = ArrayList<Pair<String, LinearLayout>>()

    override fun creer(a: MainActivity): View {
        val col = page(a)
        col.ajoute(barreHaut(a, null))
        col.ajoute(texte(a, "Qui est à table ?", 40f, 800).apply { letterSpacing = -0.02f }, 18)
        compte = texte(a, "", 16f, 400, C.MUET)
        col.ajoute(compte, 4)

        rangees = Rangees(a)
        col.ajoute(rangees, 18)

        val saisie = champ(a, if (a.store.moi.isEmpty()) "Ton prénom" else "Ex. Kenza")
        erreur = texte(a, "", 14f, 500, C.ERREUR).apply { visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        col.ajoute(etiquette(a, if (a.store.moi.isEmpty()) "Ton prénom d'abord" else "Prénom d'un ami", saisie).also { lab ->
            saisie.tag = lab
        }, 16)
        val ligneSaisie = ligne(a)
        ligneSaisie.addView(saisie, LinearLayout.LayoutParams(0, a.dpi(56), 1f))
        val ajouter = bouton(a, "Ajouter", C.LAIT, C.NUIT, 56) { ajouter(a, saisie) }
        ajouter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        ajouter.background = avecOnde(forme(C.LAIT, a.dp(16f)), 0x3315123A)
        ligneSaisie.addView(ajouter, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, a.dpi(56)).apply { marginStart = a.dpi(10) })
        col.ajoute(ligneSaisie, 8)
        col.ajoute(erreur, 6)
        saisie.imeOptions = EditorInfo.IME_ACTION_DONE
        saisie.setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_DONE) { ajouter(a, saisie); true } else false }
        auChange(saisie) { erreur.visibility = View.GONE }

        col.addView(ressort(a))
        col.ajoute(texte(a, "Comment on tranche ?", 14f, 500, C.MUET), 20)
        val grille = ligne(a)
        for ((id, nom) in listOf("roue" to "La roue", "duel" to "Le duel", "secoue" to "Secouer")) {
            val b = colonne(a).apply {
                gravity = Gravity.CENTER; isClickable = true; isFocusable = true
                setPadding(a.dpi(4), a.dpi(12), a.dpi(4), a.dpi(12))
                contentDescription = nom
                setOnClickListener { a.store.mode = id; majModes(a) }
            }
            commeRadio(b) { a.store.mode == id }
            b.addView(ImageView(a).apply {
                setImageDrawable(when (id) { "roue" -> MiniRoue(a.dp(0.8f)); "duel" -> Icone(Icone.Type.ECLAIR, C.SAFRAN, a.dp(1.1f)); else -> Icone(Icone.Type.TELEPHONE, C.SAFRAN, a.dp(1.1f)) })
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(a.dpi(28), a.dpi(28)))
            b.addView(texte(a, nom, 14f, 600).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = a.dpi(8) })
            grille.addView(b, LinearLayout.LayoutParams(0, a.dpi(92), 1f).apply { if (id != "roue") marginStart = a.dpi(8) })
            modes.add(id to b)
        }
        col.ajoute(grille, 10)

        cta = bouton(a, "", C.SAFRAN, C.NUIT) { lancer(a) }
        col.ajoute(cta, 14)
        majModes(a); majGroupe(a)
        return defilant(a, col)
    }

    override fun visible(a: MainActivity) { majGroupe(a); majModes(a) }

    private fun ajouter(a: MainActivity, e: EditText) {
        val v = e.text.toString().trim().replace(Regex("\\s+"), " ")
        val groupe = groupeComplet(a)
        when {
            v.isEmpty() -> montrerErreur("Écris un prénom avant d'appuyer sur Ajouter.")
            v.length > 14 -> montrerErreur("14 lettres maximum pour un prénom.")
            groupe.any { it.equals(v, ignoreCase = true) } -> montrerErreur("$v est déjà à table.")
            groupe.size >= 12 -> montrerErreur("12 personnes maximum autour de la table.")
            a.store.moi.isEmpty() -> {
                a.store.moi = v; e.setText("")
                (e.tag as? TextView)?.text = "Prénom d'un ami"
                e.hint = "Ex. Kenza"
                majGroupe(a)
            }
            else -> { a.store.groupe = a.store.groupe + v; e.setText(""); majGroupe(a) }
        }
    }

    private fun montrerErreur(s: String) { erreur.text = s; erreur.visibility = View.VISIBLE }

    private fun majGroupe(a: MainActivity) {
        rangees.removeAllViews()
        val moi = a.store.moi
        val groupe = groupeComplet(a)
        for ((i, nom) in groupe.withIndex()) {
            val estMoi = nom == moi
            val puce = ligne(a).apply {
                background = forme(C.fond(i), a.dp(26f), if (estMoi) C.LAIT else 0, if (estMoi) a.dpi(2) else 0)
                setPadding(a.dpi(6), a.dpi(4), if (estMoi) a.dpi(16) else a.dpi(2), a.dpi(4))
            }
            puce.addView(TextView(a).apply {
                text = nom.take(1).uppercase(); gravity = Gravity.CENTER; typeface = Polices.lalezar(a); setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTextColor(C.texteSur(i)); background = forme(0x3815123A, a.dp(20f)); setPadding(0, a.dpi(3), 0, 0)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(a.dpi(40), a.dpi(40)))
            puce.addView(texte(a, if (estMoi) "$nom (toi)" else nom, 17f, 600, C.texteSur(i)), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = a.dpi(10) })
            if (!estMoi) {
                puce.addView(boutonIcone(a, Icone.Type.FERMER, "Retirer $nom", 0, C.texteSur(i)) {
                    a.store.groupe = a.store.groupe.filter { it != nom }; majGroupe(a)
                }.apply { layoutParams = LinearLayout.LayoutParams(a.dpi(44), a.dpi(44)) })
            }
            rangees.addView(puce)
        }
        val n = groupe.size
        compte.text = when {
            moi.isEmpty() -> "Commence par ton prénom : il sert à calculer ce que tu as payé et épargné."
            n < 2 -> "Ajoute au moins un ami pour jouer."
            else -> "$n personnes à table. Le sort choisit parmi elles."
        }
    }

    private fun majModes(a: MainActivity) {
        for ((id, v) in modes) {
            val on = a.store.mode == id
            v.background = avecOnde(forme(if (on) 0x24FFB938 else C.NUIT2, a.dp(20f), if (on) C.SAFRAN else C.BORD, a.dpi(2)))
        }
        cta.text = when (a.store.mode) { "duel" -> "Lancer le duel"; "secoue" -> "Secouer le téléphone"; else -> "Lancer la roue" }
    }

    private fun lancer(a: MainActivity) {
        val g = groupeComplet(a)
        if (a.store.moi.isEmpty()) { montrerErreur("Écris d'abord ton prénom, puis appuie sur Ajouter."); return }
        if (g.size < 2) { montrerErreur("Ajoute au moins un ami pour lancer le sort."); return }
        a.aller(when (a.store.mode) { "duel" -> DuelEcran(g); "secoue" -> SecoueEcran(g); else -> RoueEcran(g) })
    }
}

// ---------- Verdict commun aux trois jeux ----------

private fun ouvrirVerdict(a: MainActivity, groupe: List<String>, payeur: String, type: String) {
    val moi = a.store.moi
    val s = Soiree(
        id = System.currentTimeMillis(), date = System.currentTimeMillis(), type = type, payeur = payeur,
        participants = groupe, nb = groupe.size, montant = 0.0,
        moiPresent = groupe.contains(moi), moiPayeur = payeur == moi,
    )
    a.store.ajouter(s)
    a.aller(VerdictEcran(s))
}

// ---------- 3. La roue ----------

class RoueEcran(private val groupe: List<String>) : Ecran() {
    private var gagnant: String? = null

    override fun creer(a: MainActivity): View {
        val col = page(a)
        col.background = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT; gradientRadius = a.dp(420f); colors = intArrayOf(0xFF241F66.toInt(), C.NUIT)
        }
        val haut = barreHaut(a, null)
        haut.addView(ressort(a).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        haut.addView(arabe(a, 24f))
        haut.addView(espaceH(a, 48))
        col.ajoute(haut)
        val titre = texte(a, "Chkoun ykhelles ?", 30f, 800).apply { gravity = Gravity.CENTER; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        val aide = texte(a, "Lance la roue d'un geste, ou appuie sur le bouton.", 15f, 400, C.MUET).apply { gravity = Gravity.CENTER }
        col.ajoute(titre, 14); col.ajoute(aide, 6)
        val roue = Roue(a).apply { noms = groupe }
        col.ajoute(roue, 18)
        col.addView(ressort(a))

        val actions = colonne(a)
        lateinit var lancerB: android.widget.Button
        fun etatInitial() {
            actions.removeAllViews()
            actions.ajoute(lancerB)
        }
        lancerB = bouton(a, "Lancer la roue", C.SAFRAN, C.NUIT, 64) { roue.lancer(1f) }
        roue.quandDepart = {
            gagnant = null; titre.text = "Chkoun ykhelles ?"; aide.text = "Ça tourne… personne ne respire."
            lancerB.isEnabled = false; lancerB.text = "Ça tourne…"; etatInitial()
        }
        roue.quandArret = { i ->
            val g = groupe[i]; gagnant = g
            roue.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            titre.text = "C'est $g qui régale !"; aide.text = "La roue a parlé. Pas de réclamation."
            lancerB.isEnabled = true; lancerB.text = "Lancer la roue"
            actions.removeAllViews()
            actions.ajoute(bouton(a, "Voir le verdict de $g", C.GRENADE, C.NUIT) { ouvrirVerdict(a, groupe, g, "roue") })
            actions.ajoute(boutonContour(a, "Revanche : relancer la roue") { roue.lancer(1f) }, 10)
        }
        etatInitial()
        col.ajoute(actions, 12)
        return col
    }
}

// ---------- 4. Le duel ----------

class DuelEcran(private val groupe: List<String>) : Ecran() {
    private enum class Phase { PRET, ATTENTE, GO, FINI }
    private var phase = Phase.PRET
    private val h = Handler(Looper.getMainLooper())
    private var t0 = 0L
    private val finalistes: List<String> = if (groupe.size == 2) groupe else groupe.shuffled().take(2)
    private var perdant = 0
    private var raison = ""
    private lateinit var moities: List<LinearLayout>
    private lateinit var messages: List<TextView>
    private lateinit var verre: VerreThe
    private lateinit var statut: TextView
    private lateinit var principal: android.widget.Button

    override fun creer(a: MainActivity): View {
        val cadre = FrameLayout(a)
        val pile = colonne(a)
        val ms = ArrayList<TextView>(); val mo = ArrayList<LinearLayout>()
        for (i in 0..1) {
            val m = colonne(a).apply {
                gravity = Gravity.CENTER; isClickable = true; isFocusable = true
                setPadding(a.dpi(24), if (i == 0) 0 else a.dpi(80), a.dpi(24), if (i == 0) a.dpi(80) else 0)
                if (i == 0) rotation = 180f
                contentDescription = "Zone de ${finalistes[i]} : touche quand le verre devient vert"
                setOnClickListener { toucher(a, i) }
                setOnTouchListener { v, e -> if (e.actionMasked == MotionEvent.ACTION_DOWN) v.performClick(); true }
            }
            m.addView(texte(a, finalistes[i], 16f, 500, C.MUET).apply { gravity = Gravity.CENTER })
            val msg = texte(a, "", 30f, 800).apply { gravity = Gravity.CENTER }
            m.addView(msg, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = a.dpi(8) })
            pile.addView(m, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            ms.add(msg); mo.add(m)
        }
        messages = ms; moities = mo
        cadre.addView(pile, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val centre = colonne(a).apply { gravity = Gravity.CENTER_HORIZONTAL }
        verre = VerreThe(a)
        centre.addView(verre, LinearLayout.LayoutParams(a.dpi(132), a.dpi(132)))
        statut = texte(a, "", 15f, 600).apply {
            gravity = Gravity.CENTER; background = forme(0xD915123A.toInt(), a.dp(20f)); setPadding(a.dpi(16), a.dpi(8), a.dpi(16), a.dpi(8))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
        }
        centre.addView(statut, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = a.dpi(12) })
        cadre.addView(centre, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        val bas = ligne(a).apply { setPadding(a.dpi(16), 0, a.dpi(16), a.dpi(20)) }
        bas.addView(boutonIcone(a, Icone.Type.RETOUR, "Retour", 0xD915123A.toInt()) { a.retour() }.apply { layoutParams = LinearLayout.LayoutParams(a.dpi(52), a.dpi(52)) })
        principal = bouton(a, "Lancer le duel", C.SAFRAN, C.NUIT, 52) { action(a) }
        bas.addView(principal, LinearLayout.LayoutParams(0, a.dpi(52), 1f).apply { marginStart = a.dpi(10) })
        cadre.addView(bas, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        maj()
        return cadre
    }

    private fun action(a: MainActivity) {
        when (phase) {
            Phase.PRET -> {
                phase = Phase.ATTENTE; maj()
                h.postDelayed({ if (phase == Phase.ATTENTE) { t0 = SystemClock.uptimeMillis(); phase = Phase.GO; verre.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); maj() } }, 1400L + Random.nextLong(2200L))
            }
            Phase.FINI -> ouvrirVerdict(a, groupe, finalistes[perdant], "duel")
            else -> {}
        }
    }

    private fun toucher(a: MainActivity, qui: Int) {
        when (phase) {
            Phase.ATTENTE -> { h.removeCallbacksAndMessages(null); perdant = qui; raison = "tôt"; phase = Phase.FINI; maj() }
            Phase.GO -> { raison = "${SystemClock.uptimeMillis() - t0} ms"; perdant = 1 - qui; phase = Phase.FINI; maj() }
            else -> {}
        }
    }

    private fun maj() {
        for (i in 0..1) {
            messages[i].text = when (phase) {
                Phase.PRET -> "Pose ton pouce ici"
                Phase.ATTENTE -> "Attends le vert…"
                Phase.GO -> "TOUCHE !"
                Phase.FINI -> if (i == perdant) (if (raison == "tôt") "Trop tôt ! Tu régales." else "Trop lent. Tu régales.") else (if (raison == "tôt") "Gagné sans bouger." else "Gagné en $raison !")
            }
            moities[i].setBackgroundColor(when (phase) {
                Phase.FINI -> if (i == perdant) 0xFFB8344B.toInt() else 0xFF1E5C4C.toInt()
                Phase.GO -> 0xFF1F4D45.toInt()
                else -> if (i == 0) 0xFF221D5C.toInt() else 0xFF2B2575.toInt()
            })
        }
        val go = phase == Phase.GO || phase == Phase.FINI
        if (phase == Phase.GO && animationsOk()) verre.animate().scaleX(1.18f).scaleY(1.18f).setDuration(120).withEndAction { verre.animate().scaleX(1f).scaleY(1f).setDuration(180).start() }.start()
        if (phase == Phase.ATTENTE && animationsOk()) verre.animate().rotation(-6f).setDuration(90).withEndAction { verre.animate().rotation(6f).setDuration(180).withEndAction { verre.animate().rotation(0f).setDuration(90).start() }.start() }.start()
        verre.couleurThe = if (go) C.MENTHE else C.GRENADE
        verre.anneau = if (phase == Phase.GO) C.MENTHE else C.SAFRAN
        statut.text = when (phase) {
            Phase.PRET -> if (groupe.size > 2) "Le sort a choisi ${finalistes[0]} et ${finalistes[1]}" else "Le verre devient vert : le plus rapide touche"
            Phase.ATTENTE -> "Le thé est trop chaud…"
            Phase.GO -> "Le thé est prêt !"
            Phase.FINI -> "${finalistes[perdant]} paie l'addition"
        }
        principal.isEnabled = phase == Phase.PRET || phase == Phase.FINI
        principal.text = when (phase) { Phase.PRET -> "Lancer le duel"; Phase.FINI -> "Voir le verdict"; else -> "Duel en cours…" }
    }

    override fun cache(a: MainActivity) {
        if (phase == Phase.ATTENTE || phase == Phase.GO) { h.removeCallbacksAndMessages(null); phase = Phase.PRET; maj() }
    }
}

// ---------- 5. Secouer le téléphone ----------

class SecoueEcran(private val groupe: List<String>) : Ecran(), SensorEventListener {
    private var capteurs: SensorManager? = null
    private val h = Handler(Looper.getMainLooper())
    private var enCours = false
    private var dernierSecouage = 0L
    private lateinit var nom: TextView
    private lateinit var aide: TextView
    private lateinit var actions: LinearLayout
    private var activite: MainActivity? = null
    private var secousse: android.animation.ObjectAnimator? = null

    override fun creer(a: MainActivity): View {
        activite = a
        val col = page(a)
        val haut = barreHaut(a, null)
        haut.addView(ressort(a).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        haut.addView(arabe(a, 24f))
        haut.addView(espaceH(a, 48))
        col.ajoute(haut)
        col.addView(ressort(a))
        val tel = ImageView(a).apply { setImageDrawable(Icone(Icone.Type.TELEPHONE, C.SAFRAN, a.dp(3.2f))); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        col.addView(tel, LinearLayout.LayoutParams(a.dpi(96), a.dpi(96)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        if (animationsOk()) secousse = android.animation.ObjectAnimator.ofFloat(tel, "rotation", 0f, -14f, 14f, -10f, 10f, 0f, 0f, 0f).apply {
            duration = 1600; repeatCount = android.animation.ValueAnimator.INFINITE
        }
        aide = texte(a, "Secoue le téléphone pour désigner celui qui régale.", 18f, 500, C.MUET).apply { gravity = Gravity.CENTER }
        col.ajoute(aide, 16)
        nom = texte(a, "?", 64f, 900).apply {
            gravity = Gravity.CENTER; maxLines = 1; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setAutoSizeTextTypeUniformWithConfiguration(28, 64, 2, TypedValue.COMPLEX_UNIT_SP)
        }
        col.ajoute(nom.apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, a.dpi(96)) }, 24)
        col.addView(ressort(a))
        actions = colonne(a)
        col.ajoute(actions)
        etatInitial(a)
        return col
    }

    private fun etatInitial(a: MainActivity) {
        actions.removeAllViews()
        actions.ajoute(bouton(a, "Pas envie de secouer ? Appuie ici", C.SAFRAN, C.NUIT) { tirer(a) })
    }

    private fun tirer(a: MainActivity) {
        if (enCours) return
        enCours = true
        actions.removeAllViews()
        aide.text = "Le sort hésite…"
        val gagnant = Random.nextInt(groupe.size)
        if (!animationsOk()) { fin(a, gagnant); return }
        var delai = 45L; var i = Random.nextInt(groupe.size); var total = 0L
        val pas = object : Runnable {
            override fun run() {
                i = (i + 1) % groupe.size
                nom.text = groupe[i]; nom.setTextColor(C.fond(i).let { if (it == C.MAJORELLE) C.LILAS else it })
                nom.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                total += delai; delai = (delai * 1.12f).toLong()
                if (total > 2300 && i == gagnant) fin(a, gagnant) else h.postDelayed(this, delai)
            }
        }
        h.post(pas)
    }

    private fun fin(a: MainActivity, i: Int) {
        val g = groupe[i]
        nom.text = g; nom.setTextColor(C.SAFRAN)
        nom.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        aide.text = "Le téléphone a parlé : c'est $g qui régale !"
        actions.removeAllViews()
        actions.ajoute(bouton(a, "Voir le verdict de $g", C.GRENADE, C.NUIT) { ouvrirVerdict(a, groupe, g, "secoue") })
        actions.ajoute(boutonContour(a, "Recommencer") { enCours = false; nom.text = "?"; nom.setTextColor(C.LAIT); aide.text = "Secoue le téléphone pour désigner celui qui régale."; etatInitial(a) }, 10)
    }

    override fun visible(a: MainActivity) {
        secousse?.let { if (it.isPaused) it.resume() else if (!it.isStarted) it.start() }
        val sm = a.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        capteurs = sm
        sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
    }

    override fun cache(a: MainActivity) {
        secousse?.pause()
        capteurs?.unregisterListener(this); capteurs = null
        h.removeCallbacksAndMessages(null)
        if (enCours && nom.text == "?") enCours = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / SensorManager.GRAVITY_EARTH
        val now = SystemClock.uptimeMillis()
        if (g > 2.3f && now - dernierSecouage > 800) { dernierSecouage = now; activite?.let { tirer(it) } }
    }

    override fun onAccuracyChanged(s: Sensor?, p: Int) {}
}

// ---------- 6. Le verdict ----------

class VerdictEcran(private var soiree: Soiree) : Ecran() {
    override fun creer(a: MainActivity): View {
        val cadre = FrameLayout(a)
        cadre.addView(ImageView(a).apply {
            setImageResource(R.drawable.terrasse); scaleType = ImageView.ScaleType.CENTER_CROP
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        cadre.addView(View(a).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x8C15123A.toInt(), 0x4015123A, 0xF215123A.toInt(), C.NUIT))
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val col = page(a)
        val haut = ligne(a)
        haut.addView(boutonIcone(a, Icone.Type.FERMER, "Revenir à l'accueil", 0x9915123A.toInt()) { a.accueil() })
        haut.addView(ressort(a).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        haut.addView(texte(a, "Enregistré dans ton historique", 13f, 500).apply { background = forme(0x9915123A.toInt(), a.dp(18f)); setPadding(a.dpi(14), a.dpi(8), a.dpi(14), a.dpi(8)) })
        col.ajoute(haut)

        val ticket = VueTicket(a)
        fun verdict() = Verdict(soiree.payeur ?: "?", soiree.nb, soiree.libelleMode(), soiree.montant, soiree.date)
        ticket.verdict = verdict()
        col.ajoute(ticket, 10)

        val montant = champ(a, "Ex. 348,00", nombre = true)
        if (soiree.montant > 0) montant.setText(Format.dh(soiree.montant).replace(' ', ' '))
        col.ajoute(etiquette(a, "Montant de l'addition en DH (facultatif, pour ton bilan)", montant), 6)
        col.ajoute(montant, 8)
        auChange(montant) { s ->
            val v = Format.lire(s) ?: 0.0
            soiree = soiree.copy(montant = v)
            a.store.majMontant(soiree.id, v)
            ticket.verdict = verdict()
        }
        col.addView(ressort(a))
        col.ajoute(bouton(a, "Partager le verdict", C.SAFRAN, C.NUIT) {
            try { DessinTicket.partager(a, verdict()) } catch (e: Exception) { Toast.makeText(a, "Le partage n'a pas pu s'ouvrir. Réessaie dans un instant.", Toast.LENGTH_LONG).show() }
        }, 16)
        val deux = ligne(a)
        deux.addView(boutonContour(a, "Revanche") { a.retour() }, LinearLayout.LayoutParams(0, a.dpi(52), 1f))
        deux.addView(boutonContour(a, "Partager l'addition") { a.aller(AdditionEcran(soiree.nb, soiree.montant)) }, LinearLayout.LayoutParams(0, a.dpi(52), 1f).apply { marginStart = a.dpi(10) })
        col.ajoute(deux, 10)
        cadre.addView(defilant(a, col), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val pluie = PluieDePieces(a)
        cadre.addView(pluie, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (animationsOk()) {
            // Le ticket tombe en rebondissant, puis les pièces pleuvent.
            ticket.alpha = 0f; ticket.translationY = -a.dp(220f); ticket.rotation = -14f
            ticket.animate().alpha(1f).translationY(0f).rotation(0f).setStartDelay(150).setDuration(750)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.4f))
                .withEndAction { ticket.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }.start()
            pluie.post { pluie.lancer() }
        }
        return cadre
    }
}

// ---------- 7. Partager l'addition ----------

class AdditionEcran(private var nb: Int, montantInitial: Double) : Ecran() {
    private var total = montantInitial
    private var pourboire = 10
    private var enregistre = false
    private lateinit var part: TextView
    private lateinit var detail: TextView
    private lateinit var nbTexte: TextView
    private lateinit var enregistrer: android.widget.Button
    private val boutonsPb = ArrayList<Pair<Int, android.widget.Button>>()

    override fun creer(a: MainActivity): View {
        val col = page(a)
        col.ajoute(barreHaut(a, "Partager l'addition"))

        val carte = colonne(a).apply { background = forme(C.SAFRAN, a.dp(28f)); setPadding(a.dpi(22), a.dpi(20), a.dpi(22), a.dpi(20)) }
        carte.addView(texte(a, "Chacun paie", 15f, 500, C.NUIT))
        val l = ligne(a).apply { gravity = Gravity.BOTTOM }
        part = texte(a, "", 56f, 900, C.NUIT).apply { letterSpacing = -0.03f; includeFontPadding = false; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        l.addView(part)
        l.addView(TextView(a).apply { text = "DH"; typeface = Polices.lalezar(a); setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f); setTextColor(C.NUIT) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = a.dpi(8) })
        carte.addView(l)
        detail = texte(a, "", 14f, 400, 0xFF3B2A08.toInt())
        carte.addView(detail, pleineLargeur().apply { topMargin = a.dpi(6) })
        col.ajoute(carte, 18)

        val champTotal = champ(a, "Ex. 348,00", nombre = true)
        if (total > 0) champTotal.setText(Format.dh(total).replace(' ', ' '))
        col.ajoute(etiquette(a, "Montant de l'addition en DH", champTotal), 20)
        col.ajoute(champTotal, 8)
        auChange(champTotal) { total = Format.lire(it) ?: 0.0; enregistre = false; maj(a) }

        col.ajoute(texte(a, "Nombre de personnes", 14f, 500, C.MUET), 18)
        val pas = ligne(a).apply { background = forme(C.NUIT2, a.dp(32f)); setPadding(a.dpi(8), a.dpi(8), a.dpi(8), a.dpi(8)) }
        pas.addView(boutonIcone(a, Icone.Type.MOINS, "Une personne de moins", C.BORD) { nb = maxOf(2, nb - 1); enregistre = false; maj(a) })
        nbTexte = texte(a, "", 22f, 800).apply { gravity = Gravity.CENTER; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        pas.addView(nbTexte, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        pas.addView(boutonIcone(a, Icone.Type.PLUS, "Une personne de plus", C.BORD) { nb = minOf(30, nb + 1); enregistre = false; maj(a) })
        col.ajoute(pas, 8)

        col.ajoute(texte(a, "Pourboire pour le serveur", 14f, 500, C.MUET), 18)
        val pbs = ligne(a)
        for ((k, v) in listOf(0, 5, 10, 15).withIndex()) {
            val b = boutonContour(a, "$v %", 52) { pourboire = v; enregistre = false; maj(a) }
            commeRadio(b) { pourboire == v }
            pbs.addView(b, LinearLayout.LayoutParams(0, a.dpi(52), 1f).apply { if (k > 0) marginStart = a.dpi(8) })
            boutonsPb.add(v to b)
        }
        col.ajoute(pbs, 8)
        col.addView(ressort(a))

        enregistrer = bouton(a, "Enregistrer dans mon historique", C.MAJORELLE, C.BLANC) {
            if (total <= 0) { Toast.makeText(a, "Saisis d'abord le montant de l'addition.", Toast.LENGTH_SHORT).show(); return@bouton }
            val tot = total * (1 + pourboire / 100.0)
            a.store.ajouter(Soiree(System.currentTimeMillis(), System.currentTimeMillis(), "partage", null, emptyList(), nb, tot, true, false))
            enregistre = true; maj(a)
            Toast.makeText(a, "Enregistré : ta part de ${Format.dhComplet(tot / nb)} compte dans ton bilan.", Toast.LENGTH_LONG).show()
        }
        col.ajoute(enregistrer, 20)
        col.ajoute(boutonContour(a, "Laisser le sort choisir un payeur") { a.aller(AmisEcran()) }, 10)
        maj(a)
        return defilant(a, col)
    }

    private fun maj(a: MainActivity) {
        val pb = Math.round(total * pourboire) / 100.0
        part.text = Format.dh((total + pb) / nb)
        detail.text = if (total > 0) "${Format.dhComplet(total)} + ${Format.dhComplet(pb)} de pourboire, divisé par $nb" else "Saisis le montant de l'addition pour voir la part de chacun."
        nbTexte.text = "$nb personnes"
        for ((v, b) in boutonsPb) {
            val on = v == pourboire
            b.background = avecOnde(forme(if (on) 0x24FFB938 else C.NUIT2, a.dp(16f), if (on) C.SAFRAN else C.BORD, a.dpi(2)))
        }
        enregistrer.isEnabled = !enregistre
        enregistrer.text = if (enregistre) "Enregistré dans ton historique" else "Enregistrer dans mon historique"
        enregistrer.alpha = if (enregistre) 0.6f else 1f
    }
}

// ---------- 8. Mon bilan et l'historique ----------

class BilanEcran : Ecran() {
    private lateinit var contenu: LinearLayout

    override fun creer(a: MainActivity): View {
        val col = page(a)
        col.ajoute(barreHaut(a, "Mon bilan"))
        contenu = colonne(a)
        col.ajoute(contenu, 8)
        return defilant(a, col)
    }

    override fun visible(a: MainActivity) = remplir(a)

    private fun remplir(a: MainActivity) {
        contenu.removeAllViews()
        val moi = a.store.moi
        val l = ligne(a)
        l.addView(texte(a, if (moi.isEmpty()) "Ton prénom n'est pas encore enregistré" else "Bilan de $moi", 16f, 500, C.MUET), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        l.addView(boutonIcone(a, Icone.Type.CRAYON, "Modifier ton prénom") { changerPrenom(a) })
        contenu.ajoute(l)

        val b = a.store.bilan()
        val panneau = colonne(a).apply { background = forme(C.NUIT2, a.dp(28f), C.BORD, a.dpi(2)); setPadding(a.dpi(20), a.dpi(20), a.dpi(20), a.dpi(20)) }
        val deux = ligne(a).apply { gravity = Gravity.TOP }
        fun bloc(titre: String, valeur: Double, couleur: Int): LinearLayout = colonne(a).apply {
            addView(texte(a, titre, 14f, 500, C.MUET))
            fun montant(x: Double): CharSequence {
                val s = android.text.SpannableString(Format.dh(x) + " DH")
                s.setSpan(android.text.style.RelativeSizeSpan(0.5f), s.length - 3, s.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return s
            }
            val tv = texte(a, montant(valeur), 30f, 900, couleur).apply { includeFontPadding = false; maxLines = 1
                setAutoSizeTextTypeUniformWithConfiguration(16, 30, 1, TypedValue.COMPLEX_UNIT_SP) }
            addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, a.dpi(40)).apply { topMargin = a.dpi(6) })
            if (animationsOk() && valeur > 0) android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1100; interpolator = android.view.animation.DecelerateInterpolator(2.2f)
                addUpdateListener { tv.text = montant(valeur * (it.animatedValue as Float)) }
                start()
            }
        }
        deux.addView(bloc("Tu as payé", b.paye, C.GRENADE), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        deux.addView(bloc("Tu as épargné", b.epargne, C.MENTHE), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = a.dpi(16) })
        panneau.addView(deux)
        val barre = BarreBilan(a).apply { paye = b.paye; epargne = b.epargne }
        panneau.addView(barre, pleineLargeur().apply { topMargin = a.dpi(16) })
        if (animationsOk()) android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100; startDelay = 150; interpolator = android.view.animation.DecelerateInterpolator(2f)
            addUpdateListener { barre.progres = it.animatedValue as Float }
            start()
        }
        val phrase = when {
            b.nbSoirees == 0 -> "Ton bilan se remplit à chaque verdict. Ajoute le montant de l'addition pour qu'il compte."
            else -> "Grâce au sort, tu as évité de payer ${b.foisSauve} fois. Tu as régalé ${b.foisRegale} fois, sur ${b.nbSoirees} soirée${if (b.nbSoirees > 1) "s" else ""}."
        }
        panneau.addView(texte(a, phrase, 15f, 400, C.LAIT), pleineLargeur().apply { topMargin = a.dpi(14) })
        contenu.ajoute(panneau, 12)

        contenu.ajoute(texte(a, "Historique", 22f, 800), 26)
        val soirees = a.store.soirees()
        if (soirees.isEmpty()) {
            contenu.ajoute(texte(a, "Aucune soirée pour l'instant. Lance la roue à ta prochaine sortie : chaque verdict s'enregistre ici.", 16f, 400, C.MUET), 8)
            contenu.ajoute(bouton(a, "Lancer la roue", C.SAFRAN, C.NUIT) { a.aller(AmisEcran()) }, 14)
            return
        }
        contenu.ajoute(texte(a, "Touche une soirée pour ajouter ou corriger son montant.", 14f, 400, C.DISCRET), 4)
        for ((i, s) in soirees.withIndex()) {
            val v = ligneSoiree(a, s)
            contenu.ajoute(v, 10)
            if (animationsOk() && i < 8) { v.alpha = 0f; v.translationX = a.dp(30f); v.animate().alpha(1f).translationX(0f).setStartDelay(200L + i * 60L).setDuration(380).start() }
        }
        contenu.ajoute(boutonContour(a, "Effacer tout l'historique") {
            dialogue(a).setTitle("Effacer tout l'historique ?")
                .setMessage("Toutes les soirées et ton bilan seront remis à zéro. Impossible de revenir en arrière.")
                .setPositiveButton("Tout effacer") { _, _ -> a.store.toutEffacer(); remplir(a) }
                .setNegativeButton("Annuler", null).show()
        }, 24)
    }

    private fun ligneSoiree(a: MainActivity, s: Soiree): View {
        val r = ligne(a).apply {
            background = avecOnde(forme(C.NUIT2, a.dp(20f)))
            setPadding(a.dpi(14), a.dpi(12), a.dpi(6), a.dpi(12))
            isClickable = true; isFocusable = true
            setOnClickListener { changerMontant(a, s) }
        }
        val idx = if (s.type == "partage") 1 else (s.participants.indexOf(s.payeur).takeIf { it >= 0 } ?: 3)
        r.addView(TextView(a).apply {
            text = if (s.type == "partage") "÷" else (s.payeur ?: "?").take(1).uppercase()
            gravity = Gravity.CENTER; typeface = Polices.lalezar(a); setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f); setTextColor(C.texteSur(idx))
            background = forme(C.fond(idx), a.dp(20f)); setPadding(0, a.dpi(3), 0, 0); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(a.dpi(40), a.dpi(40)))
        val mid = colonne(a)
        val titre = when {
            s.type == "partage" -> "Addition partagée à ${s.nb}"
            s.moiPayeur -> "Tu as régalé"
            else -> "${s.payeur} a régalé"
        }
        mid.addView(texte(a, titre, 16f, 700).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
        val quand = "${Format.date(s.date)} à ${Format.heure(s.date)}"
        mid.addView(texte(a, if (s.type == "partage") quand else "$quand, par ${s.libelleMode()}", 13f, 400, C.MUET))
        r.addView(mid, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = a.dpi(12); marginEnd = a.dpi(8) })
        val impact = when {
            s.montant <= 0 -> texte(a, "Montant\nà ajouter", 12f, 500, C.DISCRET)
            s.epargne() > 0 -> texte(a, "+${Format.dh(s.epargne())}\népargnés", 13f, 700, C.MENTHE)
            s.mapart() > 0 -> texte(a, "−${Format.dh(s.mapart())}\npayés", 13f, 700, C.GRENADE)
            else -> texte(a, Format.dh(s.montant) + "\nDH", 13f, 500, C.MUET)
        }
        impact.gravity = Gravity.END
        r.addView(impact)
        r.addView(boutonIcone(a, Icone.Type.CORBEILLE, "Supprimer cette soirée", 0, C.MUET) {
            dialogue(a).setTitle("Supprimer cette soirée ?").setMessage("Elle disparaîtra de ton historique et de ton bilan.")
                .setPositiveButton("Supprimer") { _, _ -> a.store.supprimer(s.id); remplir(a) }
                .setNegativeButton("Annuler", null).show()
        }.apply { layoutParams = LinearLayout.LayoutParams(a.dpi(44), a.dpi(44)).apply { marginStart = a.dpi(4) } })
        r.contentDescription = "$titre, $quand. " + if (s.montant > 0) "Addition ${Format.dhComplet(s.montant)}" else "Montant non renseigné"
        return r
    }

    private fun changerMontant(a: MainActivity, s: Soiree) {
        val e = champ(a, "Ex. 348,00", nombre = true)
        if (s.montant > 0) e.setText(Format.dh(s.montant).replace(' ', ' '))
        val boite = FrameLayout(a).apply { setPadding(a.dpi(22), a.dpi(8), a.dpi(22), 0); addView(e) }
        dialogue(a).setTitle("Montant de l'addition (DH)").setView(boite)
            .setPositiveButton("Enregistrer") { _, _ -> a.store.majMontant(s.id, Format.lire(e.text.toString()) ?: 0.0); remplir(a) }
            .setNegativeButton("Annuler", null).show()
    }

    private fun changerPrenom(a: MainActivity) {
        val e = champ(a, "Ton prénom")
        e.setText(a.store.moi)
        val boite = FrameLayout(a).apply { setPadding(a.dpi(22), a.dpi(8), a.dpi(22), 0); addView(e) }
        dialogue(a).setTitle("Ton prénom").setMessage("Il sert à savoir quand c'est toi qui paies.").setView(boite)
            .setPositiveButton("Enregistrer") { _, _ ->
                val v = e.text.toString().trim()
                if (v.isNotEmpty()) a.store.moi = v
                remplir(a)
            }
            .setNegativeButton("Annuler", null).show()
    }
}
