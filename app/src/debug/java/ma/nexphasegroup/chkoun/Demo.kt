package ma.nexphasegroup.chkoun

import android.content.Intent

/**
 * Données de démonstration, UNIQUEMENT dans la version de test (jamais dans l'appli publiée).
 * Sert aux captures d'écran du Play Store :
 * adb shell am start -n ma.nexphasegroup.chkoun/.MainActivity --es demo bilan
 */
object Demo {
    fun preparer(a: MainActivity, intent: Intent?): Ecran? {
        val ecran = intent?.getStringExtra("demo") ?: return null
        val s = a.store
        s.moi = "Yassine"
        s.groupe = listOf("Salma", "Mehdi", "Kenza", "Amine")
        s.toutEffacer()
        val jour = 86_400_000L
        val maintenant = System.currentTimeMillis()
        val g = listOf("Yassine", "Salma", "Mehdi", "Kenza", "Amine")
        val l = listOf(
            Soiree(1, maintenant - 1 * jour, "roue", "Kenza", g, 5, 348.0, true, false),
            Soiree(2, maintenant - 3 * jour, "duel", "Mehdi", g, 5, 62.0, true, false),
            Soiree(3, maintenant - 6 * jour, "partage", null, emptyList(), 4, 412.5, true, false),
            Soiree(4, maintenant - 9 * jour, "roue", "Yassine", g, 5, 180.0, true, true),
            Soiree(5, maintenant - 13 * jour, "secoue", "Salma", g, 5, 95.0, true, false),
            Soiree(6, maintenant - 16 * jour, "roue", "Amine", g, 5, 240.0, true, false),
        )
        l.reversed().forEach { s.ajouter(it) }
        return when (ecran) {
            "amis" -> AmisEcran()
            "roue" -> RoueEcran(g)
            "duel" -> DuelEcran(g)
            "secoue" -> SecoueEcran(g)
            "verdict" -> VerdictEcran(l[0].copy(date = maintenant - 3_600_000L))
            "addition" -> AdditionEcran(5, 348.0)
            "bilan" -> BilanEcran()
            else -> null
        }
    }
}
