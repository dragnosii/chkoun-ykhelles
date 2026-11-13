package ma.nexphasegroup.chkoun

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/** Un écran de l'appli. Sa vue est créée une seule fois et gardée tant qu'il est dans la pile. */
abstract class Ecran {
    private var vueCache: View? = null
    fun vue(a: MainActivity): View = vueCache ?: creer(a).also { vueCache = it }
    protected abstract fun creer(a: MainActivity): View
    open fun visible(a: MainActivity) {}
    open fun cache(a: MainActivity) {}
}

class MainActivity : Activity() {
    lateinit var store: Store
    private lateinit var racine: FrameLayout
    private val pile = ArrayList<Ecran>()
    private var rappelRetour: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)

        racine = FrameLayout(this).apply { setBackgroundColor(C.NUIT) }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            racine.setOnApplyWindowInsetsListener { v, ins ->
                val i = ins.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime() or WindowInsets.Type.displayCutout())
                v.setPadding(i.left, i.top, i.right, i.bottom); WindowInsets.CONSUMED
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            racine.setOnApplyWindowInsetsListener { v, ins ->
                @Suppress("DEPRECATION")
                v.setPadding(ins.systemWindowInsetLeft, ins.systemWindowInsetTop, ins.systemWindowInsetRight, ins.systemWindowInsetBottom)
                ins
            }
        }
        setContentView(racine)

        if (Build.VERSION.SDK_INT >= 33) {
            val cb = OnBackInvokedCallback { retour() }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb)
            rappelRetour = cb
        }

        val ecranDemo = if (BuildConfig.DEBUG) Demo.preparer(this, intent) else null
        aller(Accueil())
        if (ecranDemo != null) aller(ecranDemo)
    }

    @Deprecated("Remplacé par OnBackInvokedCallback à partir d'Android 13")
    override fun onBackPressed() { retour() }

    override fun onResume() { super.onResume(); pile.lastOrNull()?.visible(this) }
    override fun onPause() { pile.lastOrNull()?.cache(this); super.onPause() }

    fun aller(e: Ecran) {
        pile.lastOrNull()?.cache(this)
        pile.add(e)
        montrer(e, 1)
    }

    fun retour() {
        if (pile.size <= 1) { finish(); return }
        pile.removeAt(pile.size - 1).cache(this)
        montrer(pile.last(), -1)
    }

    /** Revient à l'accueil en vidant la pile. */
    fun accueil() {
        pile.lastOrNull()?.cache(this)
        while (pile.size > 1) pile.removeAt(pile.size - 1)
        montrer(pile.first(), -1)
    }

    /** Remplace l'écran courant (pas de retour possible vers lui). */
    fun remplacer(e: Ecran) {
        pile.lastOrNull()?.cache(this)
        if (pile.isNotEmpty()) pile.removeAt(pile.size - 1)
        pile.add(e)
        montrer(e, 1)
    }

    private fun montrer(e: Ecran, sens: Int) {
        val v = e.vue(this)
        (v.parent as? FrameLayout)?.removeView(v)
        racine.removeAllViews()
        racine.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        if (animationsOk()) {
            v.alpha = 0f; v.translationX = dp(24f) * sens
            v.animate().alpha(1f).translationX(0f).setDuration(220).start()
        }
        e.visible(this)
        racine.requestApplyInsets()
    }
}
