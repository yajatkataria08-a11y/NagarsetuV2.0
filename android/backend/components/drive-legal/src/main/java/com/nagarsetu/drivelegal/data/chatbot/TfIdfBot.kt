package com.nagarsetu.drivelegal.data.chatbot

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Offline TF-IDF powered chatbot for DriveLegal.
 *
 * Supports:
 *  - English queries
 *  - Hinglish / regional phrasing
 *  - Challan-specific intent matching
 *  - Keyword fallback suggestions
 */
@Singleton
class TfIdfBot @Inject constructor() {

    data class QA(val question: String, val answer: String, val tags: List<String> = emptyList())

    private val corpus: List<QA> = buildCorpus()

    private val tokenized: List<List<String>> = corpus.map { tokenize(it.question) }
    private val idf: Map<String, Double> = computeIdf()

    // ── Public API ────────────────────────────────────────────────────────────

    fun query(input: String): String {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) return fallback()

        val inputVec = tfidfVector(tokens)
        val scores = tokenized.mapIndexed { i, doc ->
            i to cosineSimilarity(inputVec, tfidfVector(doc))
        }

        val best = scores.maxByOrNull { it.second }
        return if (best != null && best.second > 0.05)
            corpus[best.first].answer
        else
            fallback()
    }

    /** Return a list of all quick-chip suggestions from corpus tags */
    fun quickSuggestions(): List<String> = corpus
        .filter { it.tags.contains("quick") }
        .map { it.question }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun fallback(): String =
        "I couldn't find a specific answer. Try asking about: helmet fine, speeding challan, " +
        "drunk driving, signal jump, parking fine, insurance, or how to pay a challan. " +
        "MP Traffic helpline: 0755-2443344 | Emergency: 112"

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .replace(Regex("[^a-z0-9\\u0900-\\u097f ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 }

    private fun computeIdf(): Map<String, Double> {
        val n = corpus.size.toDouble()
        val df = mutableMapOf<String, Int>()
        tokenized.forEach { doc -> doc.toSet().forEach { df[it] = (df[it] ?: 0) + 1 } }
        return df.mapValues { (_, count) -> ln(n / count.toDouble()) }
    }

    private fun tfidfVector(tokens: List<String>): Map<String, Double> {
        val tf = mutableMapOf<String, Double>()
        tokens.forEach { tf[it] = (tf[it] ?: 0.0) + 1.0 }
        val total = tokens.size.toDouble()
        return tf.mapValues { (term, freq) -> (freq / total) * (idf[term] ?: 0.0) }
    }

    private fun cosineSimilarity(a: Map<String, Double>, b: Map<String, Double>): Double {
        val dot = a.entries.sumOf { (k, v) -> v * (b[k] ?: 0.0) }
        val magA = sqrt(a.values.sumOf { it * it })
        val magB = sqrt(b.values.sumOf { it * it })
        return if (magA == 0.0 || magB == 0.0) 0.0 else dot / (magA * magB)
    }

    // ── Corpus builder ────────────────────────────────────────────────────────

    private fun buildCorpus(): List<QA> = listOf(
        // ── Speeding ──────────────────────────────────────────────────────────
        QA("speeding fine amount", "Speeding fine in MP: ₹1,000 first offence, ₹2,000 repeat. Heavy vehicles pay 2.5× more. Section 183 MV Act.", listOf("quick")),
        QA("speed limit violation penalty", "Speed limit violation: ₹1,000 (car/bike), ₹2,500 (truck/bus). DL suspension on third offence in 12 months."),
        QA("overspeed challan payment", "Overspeed challan under Section 183. Pay at echallan.parivahan.gov.in within 60 days to avoid penalty."),

        // ── Helmet ────────────────────────────────────────────────────────────
        QA("no helmet fine", "No-helmet fine: ₹1,000 first time, ₹2,000 repeat. Applies to rider AND pillion both — each charged separately. Section 129 MV Act.", listOf("quick")),
        QA("helmet rule bike pillion", "ISI-marked helmet mandatory for both rider and pillion on two-wheelers. Fine ₹1,000 each. Section 129."),
        QA("helmet challan MP Bhopal", "Helmet challan in MP under Section 129 MV Act. First ₹1,000, repeat ₹2,000 + DL suspension."),

        // ── Signal ────────────────────────────────────────────────────────────
        QA("red light jump fine", "Red light jumping: ₹1,000 first offence, ₹5,000 + DL suspension on repeat. Camera-based e-challan. Section 119.", listOf("quick")),
        QA("traffic signal violation penalty", "Signal violation ₹1,000 in MP. E-challan sent to registered mobile number within 48 hours."),

        // ── Drunk driving ─────────────────────────────────────────────────────
        QA("drunk driving fine DUI", "Drunk driving (BAC >30mg/100ml): ₹10,000 + 6 months jail first offence. ₹15,000 + 2 years imprisonment repeat. Section 185 MV Act.", listOf("quick")),
        QA("alcohol driving penalty India", "DUI under Section 185: DL revoked on first offence. Sobriety test at police station required. Bail after clearance."),

        // ── Insurance ─────────────────────────────────────────────────────────
        QA("no insurance fine vehicle", "No insurance: ₹2,000 first, ₹4,000 repeat. Third-party insurance mandatory under Section 196. Also: 3-month DL suspension on repeat."),
        QA("vehicle insurance mandatory India", "Third-party motor insurance is mandatory (MV Act Section 196). Fine ₹2,000 if caught without it."),

        // ── Parking ───────────────────────────────────────────────────────────
        QA("wrong parking fine Bhopal", "Illegal parking: ₹500 in Bhopal. BMC may tow — extra ₹500 towing charge. Section 122 MV Act.", listOf("quick")),
        QA("no parking challan payment", "No-parking challan Section 122. Pay at traffic police office or parivahan.gov.in. Towing impound fee separate."),

        // ── No license ────────────────────────────────────────────────────────
        QA("no license fine driving", "No DL fine: ₹5,000 + possible 3-month imprisonment. Section 3/181 MV Act. Also applies to learner without L-plate.", listOf("quick")),
        QA("without driving licence penalty", "Driving without DL: ₹5,000 fine under Section 3 read with Section 181. Court proceedings possible."),

        // ── Seat belt ─────────────────────────────────────────────────────────
        QA("no seat belt fine", "No seat belt: ₹1,000 per occupant. Applies to driver and all passengers in all seats. Section 138 MV Act."),
        QA("seat belt rule all passengers", "Seat belt mandatory for everyone in vehicle. Driver liable for all. Fine ₹1,000 per person not wearing it."),

        // ── Mobile phone ──────────────────────────────────────────────────────
        QA("mobile phone while driving fine", "Phone while driving: ₹1,000 first, ₹5,000 + DL suspension repeat. Hands-free device is allowed. Section 184."),
        QA("use phone driving challan", "Mobile use challan ₹1,000 under Section 184. Repeat = ₹5,000 + DL suspended."),

        // ── Registration / RC ─────────────────────────────────────────────────
        QA("expired RC fine vehicle", "Expired RC: ₹5,000 fine. Renew at Bhopal RTO (TT Nagar) or vahan.parivahan.gov.in. Section 192 MV Act."),
        QA("vehicle RC renewal Bhopal", "Renew RC online at Vahan portal or visit Bhopal RTO, TT Nagar. Contact: 0755-2770180."),

        // ── Overloading ───────────────────────────────────────────────────────
        QA("truck overloading fine", "Overloading: ₹20,000 base + ₹2,000 per extra tonne. Vehicle detained at weigh station. Section 194 MV Act.", listOf("quick")),
        QA("overloading penalty commercial", "Overloading fine Section 194: Driver AND owner both liable. Goods cannot proceed till excess removed."),

        // ── Triple riding ─────────────────────────────────────────────────────
        QA("triple riding fine bike", "Triple riding (3 persons on 2-wheeler): ₹1,000 fine. Max 2 persons allowed. Section 128 MV Act.", listOf("quick")),

        // ── PUC / Pollution ───────────────────────────────────────────────────
        QA("no PUC certificate fine", "No PUC: ₹1,000 fine. Renew at any authorised petrol pump or PUC centre in Bhopal. Check validity date."),
        QA("pollution certificate vehicle mandatory", "Pollution Under Control (PUC) mandatory. Fine ₹1,000. Bhopal has many PUC check centres near petrol pumps."),

        // ── Documents ─────────────────────────────────────────────────────────
        QA("documents required driving vehicle", "Required: Driving Licence, RC, Third-party Insurance, PUC certificate. DigiLocker digital copies are legally valid."),
        QA("digilocker valid traffic police", "DigiLocker digital copies of DL, RC and insurance are accepted at police checkpoints under IT Act 2000."),

        // ── Minor driving ─────────────────────────────────────────────────────
        QA("minor driving under 18 fine", "Minor driving: Guardian fined ₹25,000 + vehicle RC cancelled. Minor tried in juvenile court. Section 199A MV Act."),

        // ── Challan payment ───────────────────────────────────────────────────
        QA("how to pay challan online echallan", "Pay e-challan at echallan.parivahan.gov.in. Enter challan number or vehicle number. Pay via UPI/net banking/debit card.", listOf("quick")),
        QA("challan payment offline Bhopal", "Pay offline at MP Police e-Seva kiosk, traffic police office, or authorised bank branches in Bhopal."),
        QA("check pending challan vehicle", "Check pending challans: echallan.parivahan.gov.in → 'Check Challan Status' → enter vehicle number."),
        QA("challan due date late fine", "Pay challan within 60 days. After 90 days: 50% penalty added. Unpaid challans block RC renewal at RTO."),

        // ── Bhopal RTO ────────────────────────────────────────────────────────
        QA("Bhopal RTO contact address", "Bhopal RTO: TT Nagar, Bhopal 462003. Phone: 0755-2770180. Hours: Mon–Fri 10AM–5PM."),
        QA("MP traffic police helpline", "MP Traffic Police helpline: 0755-2443344. Emergency: 112. Traffic control room: 100."),
        QA("traffic court Bhopal dispute", "Traffic court Bhopal near TT Nagar Police Station. Challan dispute hearings: Monday–Friday 11AM–1PM."),

        // ── Hinglish ──────────────────────────────────────────────────────────
        QA("helmet fine kitna hai", "Bina helmet ke: ₹1,000 pehli baar, ₹2,000 repeat. Rider aur pillion dono ke liye alag alag. Section 129.", listOf("quick")),
        QA("challan kaise bhare online", "Challan online bharne ke liye echallan.parivahan.gov.in pe jaayein. Vehicle number ya challan number enter karein, UPI se pay karein.", listOf("quick")),
        QA("speeding ka fine kitna hai", "Speeding ka fine ₹1,000 (car/bike) aur ₹2,500 (truck) hai MP mein. Repeat offence pe ₹2,000. Section 183."),
        QA("nasha karke gaadi chalana fine", "Drunk driving mein ₹10,000 fine + 6 mahine jail pehli baar. Repeat mein ₹15,000 + 2 saal. Section 185.", listOf("quick")),
        QA("gaadi ka insurance nahi fine", "Bina insurance ke: ₹2,000 pehli baar, ₹4,000 dobara. Third-party insurance compulsory hai. Section 196."),
        QA("bina license ke gaadi chalana fine", "Bina DL ke driving: ₹5,000 fine (Section 3/181). 3 mahine ki saza bhi ho sakti hai."),
        QA("challan ka status check karo", "Challan status dekhne ke liye echallan.parivahan.gov.in pe jaayein → vehicle number daalein."),
        QA("RC expired fine kitna", "Expired RC: ₹5,000 fine. Nearest RTO mein jaayein ya Vahan portal par renew karein."),
        QA("signal todna fine kitna hai", "Red light jump: ₹1,000. E-challan registered mobile pe aata hai. Repeat pe ₹5,000 + DL suspend.", listOf("quick")),
        QA("mobile challan gaadi chalaate hue", "Mobile use karte hue drive karna: ₹1,000 fine. Hands-free allowed. Repeat pe ₹5,000."),
        QA("galat parking fine bhopal", "Wrong parking: ₹500. Towing bhi ho sakti hai + ₹500 towing charge. Parivahan site pe pay karein."),
        QA("seat belt nahi lagaya fine", "Bina seat belt: ₹1,000 per person. Driver aur sabhi passengers applicable. Section 138."),
        QA("PUC certificate nahi fine", "PUC certificate nahi: ₹1,000 fine. Nearest petrol pump ya PUC centre se banwaayein."),
        QA("overloading truck fine kitna", "Truck mein overloading: ₹20,000 + ₹2,000 per extra tonne. Gaadi detained jab tak maal na hatao.", listOf("quick")),
        QA("triple riding fine bike do se zyada", "Teen log bike pe: ₹1,000 fine. Sirf 2 log allowed. Section 128 MV Act."),
        QA("documents police check kya chahiye", "Police ke liye: DL, RC, Insurance, PUC dikhayein. DigiLocker copies bhi valid hain."),
        QA("challan kitne din mein bharna hai", "Challan 60 din mein bharna zaroori. 90 din baad 50% penalty. RC renewal bhi block ho jaata hai."),
        QA("parivahan gov in online challan", "Parivahan.gov.in ya echallan.parivahan.gov.in pe challan check aur pay kar sakte hain. Vehicle number daalen."),

        // ── Emergency / quick help ────────────────────────────────────────────
        QA("traffic police helpline number", "MP Traffic Police: 0755-2443344 | Emergency: 112 | Bhopal RTO: 0755-2770180"),
        QA("emergency number India traffic", "Emergency: 112 (all India). Ambulance: 108. Police: 100. MP Traffic: 0755-2443344."),

        // ── BIMSTEC specific ─────────────────────────────────────────────────
        QA("Bangladesh traffic fine", "Bangladesh: No helmet ৳500, Speeding ৳2,000, Drunk driving ৳5,000. Emergency: 999."),
        QA("Nepal traffic fine", "Nepal: No helmet Rs 500, Speeding Rs 1,000, DL fine Rs 2,000. Traffic: 103."),
        QA("Thailand traffic fine", "Thailand: No helmet ฿500, Speeding ฿1,000, DUI ฿10,000. Emergency: 1669."),
        QA("Sri Lanka traffic fine", "Sri Lanka: No helmet Rs 2,500, Signal jump Rs 3,000, DUI Rs 15,000. Traffic: 1990.")
    )
}
