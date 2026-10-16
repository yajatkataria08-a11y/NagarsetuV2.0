package com.nagarsetu.backend.core.assistant

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nagarsetu.backend.core.data.CivicDataHub
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import com.nagarsetu.backend.core.CivicConstants

@Singleton
class NagarSetuAssistant @Inject constructor(
    private val hub: CivicDataHub,
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val knowledge: List<KnowledgeEntry> = buildKnowledge()
    private val intentKeywords: Map<CivicIntent, List<String>> = mapOf(
        CivicIntent.PARKING to listOf("park", "parking", "slot", "hold", "qr", "mall parking"),
        CivicIntent.CHARGING to listOf("charge", "ev", "charger", "soc", "kwh", "station"),
        CivicIntent.EMERGENCY to listOf("sos", "emergency", "ambulance", "112", "108", "golden hour", "trauma"),
        CivicIntent.REPORT to listOf("report", "complaint", "civic", "garbage", "drain", "encroach"),
        CivicIntent.ROAD to listOf("pothole", "road", "streetlight", "broken road", "heatmap"),
        CivicIntent.LEGAL to listOf("challan", "fine", "helmet", "speed", "traffic", "legal", "ocr", "plate"),
        CivicIntent.HEALTH to listOf("doctor", "telemedicine", "dengue", "malaria", "epidemic", "health", "jitsi"),
        CivicIntent.ROUTE to listOf("bus", "route", "eco", "cycle", "gtfs", "navigate", "co2", "green"),
        CivicIntent.PREDICTIVE to listOf("forecast", "risk", "predict", "rag", "flood", "crime"),
        CivicIntent.RAKSHA to listOf("raksha", "safe", "shake", "fake call", "trusted contact", "location share")
    )

    fun greet(): AssistantReply = AssistantReply(
        answer = "Namaste! I'm **NagarSetu AI** — your Bhopal civic companion. " +
            "Ask about parking, SOS, challans, road reports, EV charging, health, or eco-routes. " +
            "Try: \"Where can I park near MP Nagar?\" or \"Helmet fine kitna hai?\"",
        suggestedRoute = null,
        sources = listOf("NagarSetu Civic Knowledge Base"),
        confidence = 1f
    )

    fun reply(userQuery: String): AssistantReply {
        val q = userQuery.trim()
        if (q.isBlank()) return AssistantReply("Please type a question about Bhopal civic services.", confidence = 0.5f)

        val intent = detectIntent(q.lowercase())
        val contextAnswer = contextualAnswer(intent, q)
        if (contextAnswer != null) return contextAnswer

        val best = findBestKnowledge(q)
        if (best != null) {
            return AssistantReply(
                answer = best.entry.answer,
                suggestedRoute = best.entry.route,
                sources = best.entry.sources,
                confidence = best.score.toFloat().coerceIn(0.55f, 0.98f)
            )
        }

        return AssistantReply(
            answer = "I couldn't find an exact match. " + hub.citySummary() +
                " Open **${intent.name}** from the bottom bar or rephrase your question.",
            suggestedRoute = intent.toRoute(),
            sources = listOf("Fallback"),
            confidence = 0.45f
        )
    }

    fun quickPrompts(): List<Pair<String, String>> = listOf(
        "No Helmet fine?" to "helmet fine bhopal",
        "Wrong Parking fine?" to "wrong parking fine",
        "Signal jump challan?" to "red light jump fine",
        "Drunk driving fine?" to "drunk driving fine",
        "No DL fine?" to "driving without license fine",
        "Report pothole" to "report pothole road watch",
        "Bus to MP Nagar" to "green route bus mp nagar"
    )

    private fun detectIntent(q: String): CivicIntent {
        var best = CivicIntent.GENERAL
        var max = 0
        intentKeywords.forEach { (intent, keys) ->
            val score = keys.count { q.contains(it) }
            if (score > max) {
                max = score
                best = intent
            }
        }
        return best
    }

    private fun contextualAnswer(intent: CivicIntent, q: String): AssistantReply? {
        when (intent) {
            CivicIntent.PARKING -> {
                val lots = hub.parkingLots()
                if (lots.size() == 0) return null
                val first = lots[0].asJsonObject
                return AssistantReply(
                    answer = "Nearby: **${first["name"].asString}** — ${first["availableSlots"].asInt} slots free, " +
                        "₹${first["pricing"].asJsonObject["hourly"].asInt}/hr. Use **Park** tab to hold 30 min & get QR ticket.",
                    suggestedRoute = "park_ease",
                    sources = listOf("app_data.json → parkEase"),
                    confidence = 0.9f
                )
            }
            CivicIntent.CHARGING -> {
                val st = hub.chargingStations()
                if (st.size() == 0) return null
                val o = st[0].asJsonObject
                return AssistantReply(
                    answer = "**${o["name"].asString}** — ${o["availableSlots"].asInt}/${o["totalSlots"].asInt} slots, " +
                        "₹${o["pricing"].asInt}/unit. **Charge** tab starts live SOC session (+2% every 5s demo).",
                    suggestedRoute = "charge_up",
                    sources = listOf("app_data.json → chargeUp"),
                    confidence = 0.88f
                )
            }
            CivicIntent.EMERGENCY -> {
                val nums = hub.emergencyNumbers()
                return AssistantReply(
                    answer = "Tap red **SOS** (tab 3) or shake phone 3× in 1.5s. Dial: Police ${nums?.get("police")?.asString ?: "100"}, " +
                        "Ambulance ${nums?.get("ambulance")?.asString ?: "108"}. Golden Hour countdown starts on activation.",
                    suggestedRoute = "emergency",
                    sources = listOf("app_data.json → emergencyAI"),
                    confidence = 0.92f
                )
            }
            CivicIntent.REPORT -> {
                val ward = hub.nearestWard(CivicConstants.BHOPAL_LAT, CivicConstants.BHOPAL_LNG)
                return AssistantReply(
                    answer = "Use **Report an Issue** on Home or RoadWatch 4-step flow. Tickets like RI-2026-XXXX route to " +
                        "**${ward?.authorityName ?: "BMC"}** (~5 working days).",
                    suggestedRoute = "report_it",
                    sources = listOf("Ward routing"),
                    confidence = 0.87f
                )
            }
            else -> return null
        }
    }

    private fun findBestKnowledge(query: String): ScoredEntry? {
        val tokens = query.lowercase().split(Regex("\\s+"))
        var best: ScoredEntry? = null
        knowledge.forEach { entry ->
            val docTokens = entry.keywords.flatMap { it.lowercase().split(Regex("\\s+")) }
            val score = cosine(tokens, docTokens)
            if (score > (best?.score ?: 0.1) && score > 0.12) {
                best = ScoredEntry(entry, score)
            }
        }
        return best
    }

    private fun cosine(a: List<String>, b: List<String>): Double {
        val aSet = a.toSet()
        val bSet = b.toSet()
        val dot = aSet.intersect(bSet).size.toDouble()
        if (dot == 0.0) return 0.0
        return dot / (sqrt(aSet.size.toDouble()) * sqrt(bSet.size.toDouble()))
    }

    private fun buildKnowledge(): List<KnowledgeEntry> {
        val trafficRules = mapOf(
            "no helmet head safety fine" to 
                "Helmet violation fine in Bhopal is **₹1000** along with potential 3-month license disqualification. Safety Tip: Always wear ISI-certified helmets to protect against head injuries.",
            "no seatbelt car safety fine" to 
                "Driving without a seatbelt attracts a fine of **₹1000**. Safety Tip: Seatbelts reduce the risk of death by 45% in accidents.",
            "triple riding two wheeler fine" to 
                "Triple riding on a two-wheeler is illegal and carries a fine of **₹1000**.",
            "mobile phone driving distraction fine" to 
                "Using a mobile phone while driving can lead to a fine between **₹1000 to ₹5000**. Use hands-free devices or pull over safely if urgent.",
            "signal jump red light fine" to 
                "Jumping a traffic signal (Red Light violation) attracts a fine of **₹1000 to ₹5000**.",
            "overspeeding speed limit fine" to 
                "Overspeeding fines: ₹1000-₹2000 for Light Motor Vehicles and ₹2000-₹4000 for Medium/Heavy vehicles. Keep an eye on Bhopal's speed limit signs!",
            "wrong parking towing fine" to 
                "Wrong parking fine is **₹500**. Vehicles may also be towed by Bhopal Traffic Police causing additional charges.",
            "without license driving fine" to 
                "Driving without a valid driving license (DL) carries a heavy fine of **₹5000**.",
            "no rc registration certificate fine" to 
                "Driving without a valid Registration Certificate (RC) can cost you **₹2000 to ₹5000**.",
            "no insurance vehicle policy fine" to 
                "Driving without valid vehicle insurance attracts a fine of **₹2000**.",
            "no puc pollution certificate fine" to 
                "Driving without a valid Pollution Under Control (PUC) certificate carries a heavy penalty of up(1) to **₹10000**.",
            "drunk driving alcohol fine" to 
                "Driving under the influence of alcohol (Drunk Driving) is a serious offense with a fine of **₹10000** and/or imprisonment.",
            "dangerous driving rash driving fine" to 
                "Dangerous or Rash driving attracts a fine of **₹1000 to ₹5000**.",
            "illegal u-turn wrong turn fine" to 
                "Taking an illegal U-turn in prohibited zones attracts a fine of **₹500**.",
            "wrong side driving opposite lane fine" to 
                "Driving on the wrong side of the road is extremely dangerous and attracts a fine of **₹5000**.",
            "number plate violation hspr fine" to 
                "Faulty or missing number plates (Non-HSRP) can lead to a fine between **₹500 to ₹5000**."
        )

        val base = linkedMapOf(
            "parking hold qr ticket bhopal" to
                "Hold a slot 30 minutes; QR encodes ticket ID for gate scan. Open Park tab → Hold (30m).",
            "challan kaise bhare pay fine mp" to
                "Pay via echallan.parivahan.gov.in or Drive Legal chat. Helmet/speed/signal fines vary by vehicle.",
            "report pothole ticket road watch" to
                "RoadWatch: photo → AI verify (TFLite) → GPS → ticket RW-2026-XXXX, SLA 48h.",
            "sos shake emergency raksha" to
                "Raksha: 180dp SOS + shake 3×/1.5s; fake call decoy; share location with trusted contacts.",
            "ev charging station khajuri tata" to
                "ChargeUp lists Ola/Tata/IOCL stations from seed data; start session for live SOC %.",
            "green route bus br-101 mp nagar" to
                "Green Route: BUS/CYCLE/WALK/AUTO cards with CO₂ saved; Navigate opens Google Maps.",
            "dengue malaria epidemic health" to
                "HealthWatch epidemic OSM overlay + telemedicine doctors; Jitsi mock call when online.",
            "7 day forecast risk predictive rag" to
                "Predictive: 7-day ACCIDENT/FLOOD/CRIME/HEALTH forecast + civic RAG query box.",
            "drive legal ocr number plate" to
                "Drive Legal: TfIdf FAQ 50+ (Hinglish) + ML Kit OCR for plate scan.",
            "theme dark light change appearance" to
                "Open Home → palette icon: Civic Light, Dark, Eco Green, Sunset, High Contrast themes.",
            "ward authority complaint bmc" to
                "13 Bhopal wards in app_data.json; reports auto-route to nearest ward authority.",
            "crime_data predictive hotspot" to
                "Predictive module uses seed risk scores; Thursday ACCIDENT risk often highest in demo data.",
            "wifi air quality streetlight nagarsetu home" to
                "Home dashboard: crisis bar, 4 map modes, live alerts, quick categories, service cards.",
            "fake call mom raksha safety" to
                "Raksha fake call shows 'Mom' incoming overlay — Answer/Reject to exit.",
            "golden hour trauma narmada" to
                "Emergency AI: 3600s Golden Hour timer; nearest Narmada Trauma / JP Hospital from seed.",
            "bimstec dial 112 108 100" to
                "Emergency screen: quick dial 112, 108, 100, 101, 102 with BIMSTEC labels.",
            "park ease db city mall rate" to
                "DB City Mall ~₹30/hr, 800 slots; occupancy bar turns red above 90%.",
            "rani kamlapati parking station" to
                "Rani Kamlapati Station parking 24×7, ₹15/hr in ParkEase list.",
            "triage accident blood unconscious" to
                "Emergency triage NLP: CRITICAL keywords → priority + medical plan before dispatch."
        )

        val combined = base + trafficRules

        return combined.map { (k, v) ->
            KnowledgeEntry(
                keywords = k.split(" "),
                answer = v,
                route = routeForKeywords(k),
                sources = listOf("NagarSetu Knowledge Base")
            )
        } + loadIntentJsonExtras()
    }

    private fun loadIntentJsonExtras(): List<KnowledgeEntry> {
        return try {
            val json = context.assets.open("intents.json").bufferedReader().use { it.readText() }
            val obj = gson.fromJson(json, JsonObject::class.java)
            obj.entrySet().flatMap { (module, el) ->
                val arr = el.asJsonArray ?: return@flatMap emptyList()
                arr.map { phrase ->
                    KnowledgeEntry(
                        keywords = phrase.asString.split(" "),
                        answer = "For **$module**, open the matching tab or ask a specific question. ${hub.citySummary()}",
                        route = moduleToRoute(module),
                        sources = listOf("intents.json")
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun routeForKeywords(k: String): String? = when {
        k.contains("park") -> "park_ease"
        k.contains("challan") || k.contains("helmet") || k.contains("legal") -> "drive_legal"
        k.contains("pothole") || k.contains("road") -> "road_watch"
        k.contains("sos") || k.contains("emergency") -> "emergency"
        k.contains("charge") || k.contains("ev") -> "charge_up"
        k.contains("health") || k.contains("dengue") -> "health_watch"
        k.contains("route") || k.contains("bus") -> "green_route"
        k.contains("forecast") || k.contains("predictive") -> "predictive"
        k.contains("raksha") || k.contains("fake") -> "raksha"
        k.contains("report") -> "report_it"
        else -> null
    }

    private fun moduleToRoute(module: String): String? = when (module) {
        "driveLegal", "drive_legal" -> "drive_legal"
        "reportIt", "report_it" -> "report_it"
        "parkEase", "park_ease" -> "park_ease"
        "chargeUp", "charge_up" -> "charge_up"
        "greenRoute", "green_route" -> "green_route"
        "healthWatch", "health_watch" -> "health_watch"
        "emergency", "emergencyAI" -> "emergency"
        "roadWatch", "road_watch" -> "road_watch"
        "predictive" -> "predictive"
        "raksha" -> "raksha"
        else -> "dashboard"
    }

    private data class KnowledgeEntry(
        val keywords: List<String>,
        val answer: String,
        val route: String?,
        val sources: List<String>
    )

    private data class ScoredEntry(val entry: KnowledgeEntry, val score: Double)

    private fun CivicIntent.toRoute(): String? = when (this) {
        CivicIntent.PARKING -> "park_ease"
        CivicIntent.CHARGING -> "charge_up"
        CivicIntent.EMERGENCY -> "emergency"
        CivicIntent.REPORT -> "report_it"
        CivicIntent.ROAD -> "road_watch"
        CivicIntent.LEGAL -> "drive_legal"
        CivicIntent.HEALTH -> "health_watch"
        CivicIntent.ROUTE -> "green_route"
        CivicIntent.PREDICTIVE -> "predictive"
        CivicIntent.RAKSHA -> "raksha"
        CivicIntent.GENERAL -> "dashboard"
    }
}
