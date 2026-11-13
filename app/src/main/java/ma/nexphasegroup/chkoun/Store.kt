package ma.nexphasegroup.chkoun

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Une soirée : un verdict (roue, duel, téléphone secoué) ou une addition partagée. */
data class Soiree(
    val id: Long,
    val date: Long,
    val type: String,            // "roue", "duel", "secoue" ou "partage"
    val payeur: String?,         // la personne désignée (null pour un partage)
    val participants: List<String>,
    val nb: Int,                 // nombre de personnes à table
    var montant: Double,         // addition totale, pourboire compris (0 = non renseignée)
    val moiPresent: Boolean,
    val moiPayeur: Boolean,
) {
    fun json(): JSONObject = JSONObject().apply {
        put("id", id); put("date", date); put("type", type); put("payeur", payeur ?: JSONObject.NULL)
        put("participants", JSONArray(participants)); put("nb", nb); put("montant", montant)
        put("moiPresent", moiPresent); put("moiPayeur", moiPayeur)
    }

    /** Ce que j'ai payé pour cette soirée. */
    fun mapart(): Double = when {
        !moiPresent || montant <= 0 -> 0.0
        type == "partage" -> montant / nb
        moiPayeur -> montant
        else -> 0.0
    }

    /** Ce que le sort m'a fait économiser : la part que j'aurais payée si on avait partagé. */
    fun epargne(): Double = if (type != "partage" && moiPresent && !moiPayeur && montant > 0) montant / nb else 0.0

    fun libelleMode(): String = when (type) {
        "roue" -> "la roue"
        "duel" -> "le duel"
        "secoue" -> "le téléphone secoué"
        else -> "partage"
    }

    companion object {
        fun depuis(o: JSONObject): Soiree {
            val arr = o.optJSONArray("participants") ?: JSONArray()
            return Soiree(
                id = o.getLong("id"), date = o.getLong("date"), type = o.getString("type"),
                payeur = if (o.isNull("payeur")) null else o.optString("payeur"),
                participants = List(arr.length()) { arr.getString(it) },
                nb = o.optInt("nb", maxOf(1, arr.length())), montant = o.optDouble("montant", 0.0),
                moiPresent = o.optBoolean("moiPresent", true), moiPayeur = o.optBoolean("moiPayeur", false),
            )
        }
    }
}

data class Bilan(val paye: Double, val epargne: Double, val foisRegale: Int, val foisSauve: Int, val nbSoirees: Int)

/** Tout est enregistré sur le téléphone uniquement (aucun envoi, aucun compte). */
class Store(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("chkoun", Context.MODE_PRIVATE)

    var moi: String
        get() = prefs.getString("moi", "") ?: ""
        set(v) { prefs.edit().putString("moi", v.trim().take(14)).apply() }

    var groupe: List<String>
        get() {
            val arr = try { JSONArray(prefs.getString("groupe", "[]")) } catch (e: Exception) { JSONArray() }
            return List(arr.length()) { arr.getString(it) }
        }
        set(v) { prefs.edit().putString("groupe", JSONArray(v).toString()).apply() }

    var mode: String
        get() = prefs.getString("mode", "roue") ?: "roue"
        set(v) { prefs.edit().putString("mode", v).apply() }

    fun soirees(): List<Soiree> {
        val arr = try { JSONArray(prefs.getString("soirees", "[]")) } catch (e: Exception) { JSONArray() }
        val l = ArrayList<Soiree>()
        for (i in 0 until arr.length()) try { l.add(Soiree.depuis(arr.getJSONObject(i))) } catch (_: Exception) {}
        return l.sortedByDescending { it.date }
    }

    private fun ecrire(l: List<Soiree>) {
        val arr = JSONArray(); l.take(500).forEach { arr.put(it.json()) }
        prefs.edit().putString("soirees", arr.toString()).apply()
    }

    fun ajouter(s: Soiree) = ecrire(listOf(s) + soirees())
    fun majMontant(id: Long, montant: Double) = ecrire(soirees().map { if (it.id == id) it.copy(montant = montant) else it })
    fun supprimer(id: Long) = ecrire(soirees().filter { it.id != id })
    fun toutEffacer() = prefs.edit().remove("soirees").apply()

    fun bilan(): Bilan {
        val l = soirees()
        return Bilan(
            paye = l.sumOf { it.mapart() },
            epargne = l.sumOf { it.epargne() },
            foisRegale = l.count { it.type != "partage" && it.moiPayeur },
            foisSauve = l.count { it.type != "partage" && it.moiPresent && !it.moiPayeur },
            nbSoirees = l.size,
        )
    }
}

object Format {
    private const val ESP = ' '

    /** 1234.5 -> "1 234,50" (espaces insécables). */
    fun dh(v: Double): String {
        val cent = Math.round(v * 100)
        val neg = cent < 0
        val abs = Math.abs(cent)
        val ent = (abs / 100).toString()
        val dec = (abs % 100).toString().padStart(2, '0')
        val sb = StringBuilder()
        for ((i, c) in ent.withIndex()) {
            if (i > 0 && (ent.length - i) % 3 == 0) sb.append(ESP)
            sb.append(c)
        }
        return (if (neg) "-" else "") + sb + "," + dec
    }

    fun dhComplet(v: Double) = dh(v) + ESP + "DH"

    /** Lit "1 234,5" ou "1234.50". Renvoie null si ce n'est pas un nombre. */
    fun lire(s: String): Double? {
        val t = s.replace(" ", "").replace(" ", "").replace(',', '.')
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()?.takeIf { it >= 0 && it < 1_000_000 }
    }

    fun date(ms: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(ms))
    fun heure(ms: Long): String = SimpleDateFormat("HH 'h' mm", Locale.FRANCE).format(Date(ms))
}
