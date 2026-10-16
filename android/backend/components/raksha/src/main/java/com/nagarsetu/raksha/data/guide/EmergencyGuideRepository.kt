package com.nagarsetu.raksha.data.guide

import com.nagarsetu.raksha.domain.model.EmergencyGuideItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the full offline emergency guide corpus — ported from Raksha
 * (SafePath Indore) and adapted for NagarSetu's Madhya Pradesh context.
 *
 * All 14 guide items are embedded locally so they work with zero network.
 */
@Singleton
class EmergencyGuideRepository @Inject constructor() {

    fun getGuideItems(): List<EmergencyGuideItem> = GUIDE_ITEMS

    fun search(query: String): List<EmergencyGuideItem> {
        if (query.isBlank()) return GUIDE_ITEMS
        val q = query.lowercase()
        return GUIDE_ITEMS.filter {
            it.question.lowercase().contains(q) || it.answer.lowercase().contains(q)
        }
    }

    companion object {
        val GUIDE_ITEMS = listOf(
            EmergencyGuideItem(
                emoji    = "⚠️",
                question = "What to do if I am being followed?",
                answer   = """Immediate Actions:
• Cross the street and change your direction. If they follow again, cross back.
• Go to a crowded area – a market, shop, petrol pump, or any open business.
• Enter any shop or restaurant – tell the person at the counter you need help.
• Call a friend or family member loudly – say "I'll be there in 2 minutes, someone is following me."
• Press the SOS button immediately via Raksha or shake your phone 3 times.
• Never go directly home – you don't want them to know where you live.

If you are in a vehicle:
• Take four right turns – if the same vehicle follows all four, you are definitely being followed.
• Drive to the nearest police station or crowded area."""
            ),
            EmergencyGuideItem(
                emoji    = "📞",
                question = "Helpline numbers for women in India",
                answer   = """Emergency Numbers (toll-free, 24x7):
• Police Emergency:         100 / 112
• Women's Helpline:         1091  (also 103)
• NCW Helpline:             7827170170
• Child Helpline:           1098
• Cyber Crime:              1930
• Crime Stopper:            1090
• Ambulance / Medical:      108
• Citizens' Call Centre:    155300

Madhya Pradesh Specific:
• MP Women Helpline:        0755-2661400
• Bhopal Police Control:    0755-2443500
• Indore Police Control:    0731-2422200"""
            ),
            EmergencyGuideItem(
                emoji    = "🥋",
                question = "Self-defense tips for an immediate situation",
                answer   = """Target vulnerable areas:
• Eyes – poke or scratch with keys, fingers, or any object
• Nose – palm strike upward from below
• Throat – edge-of-hand strike
• Groin – knee strike or kick

Use everyday objects as weapons:
• Keys – hold between fingers like a claw
• Pen – jab soft areas (neck, hand, arm)
• Umbrella / bag – swing or thrust
• Pepper spray – aim for eyes, spray in Z-motion, then RUN

Shout:
• Scream "FIRE" (AAG) – people respond faster to fire than to "help"

After defending yourself:
• Run to a public place or police station immediately
• Do NOT stay to fight"""
            ),
            EmergencyGuideItem(
                emoji    = "🏛️",
                question = "What are Safe Havens?",
                answer   = """Safe Havens are trusted public locations you can run to in an emergency:

• Police stations – nearest and safest option (tap map for nearest)
• Hospitals & clinics – open 24x7
• 24x7 shops – petrol pumps, pharmacies, convenience stores
• Government buildings – collectorate, bus stands, railway stations

How to use in NagarSetu Raksha:
1. Open the Live Track map tab
2. Police station markers are shown in blue
3. Tap a marker to get walking directions
4. Enter and ask for help immediately"""
            ),
            EmergencyGuideItem(
                emoji    = "⚖️",
                question = "What is the POSH Act?",
                answer   = """Full Name: Sexual Harassment of Women at Workplace (Prevention, Prohibition and Redressal) Act, 2013

Key provisions:
• Every employer with 10+ employees must have an Internal Complaints Committee (ICC)
• ICC presiding officer must be a woman
• At least 50% of ICC members must be women
• An external NGO or legal expert must be included

What qualifies as sexual harassment:
• Physical contact and advances
• Request or demand for sexual favors
• Sexually colored remarks
• Showing pornography
• Any unwelcome conduct of a sexual nature

Complaint timeline:
• File within 3 months of incident
• ICC must complete inquiry within 90 days
• Employer must implement recommendations within 60 days"""
            ),
            EmergencyGuideItem(
                emoji    = "📝",
                question = "How to file an FIR online?",
                answer   = """For Cyber Crimes (online harassment, stalking, social media threats):
Visit: https://cybercrime.gov.in
• Select "Report Cyber Crime related to Women/Child"
• Anonymous filing allowed for gender-based offenses
• Upload screenshots, URLs, chat logs as evidence
• Save the complaint reference number

For Physical Offenses (assault, following, harassment):
• Visit any police station (Zero FIR – no jurisdictional limit)
• Provide a written complaint describing the incident
• Keep a copy of the FIR receipt

For NCW Complaints:
• Visit: ncw.nic.in
• Call NCW helpline: 7827170170

Documents to bring / screenshots to save:
• Date, time, location details
• Names or descriptions of perpetrators
• Witness contact information
• Any recordings, messages, or photos"""
            ),
            EmergencyGuideItem(
                emoji    = "👀",
                question = "What to do if you see someone being harassed?",
                answer   = """The 5 D's Framework:
1. DIRECT – Safely confront the harasser: "That's not okay" or "Leave them alone"
2. DISTRACT – Interrupt the situation:
   – Spill something nearby
   – Ask for directions or the time
   – Say "I think I know you from somewhere"
3. DELEGATE – Get help:
   – Tell a bystander: "That person needs your help"
   – Alert security or staff
4. DELAY – Stay with the victim after the harasser leaves:
   – "Are you okay? Can I help you get somewhere safe?"
5. DOCUMENT – Record discreetly as evidence if it is safe to do so

Always call emergency services: Dial 100 or 112 immediately.
Your intervention can make a critical difference."""
            ),
            EmergencyGuideItem(
                emoji    = "🚌",
                question = "Safety tips for public transport",
                answer   = """Before boarding:
• Share your live location via Raksha with at least 2 contacts
• Note the vehicle number (bus, taxi, or auto registration)
• Use the in-app emergency button on ride-hailing apps

During the journey:
• Sit near the driver or conductor
• Avoid empty buses or coaches at night
• Stay alert – avoid headphones in isolated areas
• Keep your bag close at all times

If you feel unsafe:
• Change your seat to be near other passengers
• Call someone and speak loudly about your location
• Get off at a busy stop (market, hospital, police station)
• Use the SOS button in Raksha"""
            ),
            EmergencyGuideItem(
                emoji    = "🚗",
                question = "How to check if a vehicle is following you?",
                answer   = """The 4-Turn Test:
Take four consecutive right turns (or left turns). A normal driver will not replicate all four turns. If the same vehicle follows – you are being followed.

If you are driving:
• Do NOT go home
• Drive to the nearest police station
• Drive to a crowded market, mall, or petrol pump
• Call 100 and state your location

If you are walking:
• Cross the street and change direction
• Enter any shop or restaurant and alert staff
• Call someone loudly: "I'm near [location], someone is following me"
• Head to the nearest Safe Haven shown in Raksha's map

Never: go home, go to an isolated street, or stop in a dark/parking area."""
            ),
            EmergencyGuideItem(
                emoji    = "👮",
                question = "Legal rights when stopped by police",
                answer   = """Your fundamental rights under Indian law:

Right to identification:
• Police must show their ID card upon request
• They must provide their name and badge number

Right to reason:
• You can ask why you are being stopped or detained
• Police must provide a written reason if detaining you

Right to call someone:
• You have the right to call a family member or lawyer

Rights specifically for women:
• Women cannot be called to a police station after sunset and before sunrise without prior Magistrate permission (Section 160 CrPC)
• Only female officers can conduct body searches of women
• Women can file at any police station (Zero FIR)

If harassed by police:
• Note their badge number
• File with the Police Complaints Authority or the District SP
• Contact Women's Helpline: 1091"""
            ),
            EmergencyGuideItem(
                emoji    = "🌶️",
                question = "How to use pepper spray effectively?",
                answer   = """Before an incident:
• Keep it in an outside pocket – not buried in your bag
• Practice removing the safety pin quickly
• Check the expiration date regularly

How to aim:
• Target the eyes – most effective
• Hold at arm's length with thumb on actuator

Spraying technique:
• Spray in a "Z" motion across the attacker's face
• Use short 1–2 second bursts – not continuous
• Do not spray into the wind

After spraying:
• RUN immediately to a crowded area or police station
• Call 100 once you are safe
• Effect lasts 30–45 minutes (temporary blindness + breathing difficulty)

Note: Pepper spray is legal for self-defense in India."""
            ),
            EmergencyGuideItem(
                emoji    = "📜",
                question = "What is Section 354 IPC?",
                answer   = """Section 354 IPC: Assault or criminal force against a woman to outrage her modesty.

Punishment:
• Minimum: 1 year imprisonment
• Maximum: 5 years + fine
• Cognizable: Police can arrest without warrant
• Non-bailable offense

Related sections:
• 354A: Sexual harassment – up to 3 years
• 354B: Assault to disrobe – 3 to 7 years
• 354C: Voyeurism – 1 to 3 years
• 354D: Stalking – up to 3 years
• 376: Rape – minimum 10 years

How to report:
• File FIR at any police station (Zero FIR – no jurisdictional limit)
• Call 100 or 112 immediately
• Contact NCW: 7827170170"""
            ),
            EmergencyGuideItem(
                emoji    = "🎒",
                question = "Emergency kit to carry in your bag",
                answer   = """Essential daily safety items:

• Pepper spray – immediate self-defense (keep in outer pocket)
• Whistle – sound carries farther than shouting
• Power bank + cable – dead phone = no emergency help
• Portable door lock – extra security in hotels or PGs
• Written emergency contacts – in case phone battery dies
• Small flashlight – for dark areas or power cuts
• Cash ₹500–1000 – emergency travel money

Keep pepper spray and whistle accessible at all times in an outer pocket or bag strap.

Useful apps to have ready:
• NagarSetu Raksha – SOS, live track, fake call
• Google Maps – live location sharing
• Truecaller – caller ID and spam detection"""
            ),
            EmergencyGuideItem(
                emoji    = "📍",
                question = "How to share live location on WhatsApp / Google Maps?",
                answer   = """Using NagarSetu Raksha (recommended):
• Tap the Live Track card on the Raksha screen
• Toggle "Live Location" switch ON
• Your trusted contacts are notified automatically

WhatsApp Live Location:
1. Open chat → Attach (paperclip) icon
2. Select "Location" → "Share Live Location"
3. Choose duration (15 min, 1 hr, 8 hrs) → Send

Google Maps Live Location:
1. Open Google Maps → Tap your blue dot
2. Select "Share location"
3. Choose duration and select contacts → Share

When to share:
• Before getting into any vehicle
• When walking alone at night
• If you feel you are being followed
• When meeting someone new

Pro tip: Share with at least 2 contacts – if one doesn't see the notification, the other will."""
            )
        )
    }
}
