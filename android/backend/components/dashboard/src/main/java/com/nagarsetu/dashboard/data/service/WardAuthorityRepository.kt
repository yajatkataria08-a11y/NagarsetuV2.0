package com.nagarsetu.dashboard.data.service

import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.dashboard.domain.model.WardAuthority
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads ward authority KPI data from app_data.json "wards" array.
 * Exposes sorting and filtering helpers consumed by DashboardViewModel.
 */
@Singleton
class WardAuthorityRepository @Inject constructor(
    private val assets: AssetDataRepository
) {
    fun allWards(): List<WardAuthority> {
        val arr = assets.loadAppData().getAsJsonArray("wards") ?: return seedWards()
        return arr.map { el ->
            val o = el.asJsonObject
            WardAuthority(
                id            = o["id"].asString,
                wardName      = o["name"].asString,
                authorityName = o["authorityName"].asString,
                helpline      = o["authorityHelpline"].asString,
                zone          = o["zone"]?.asString ?: "Central",
                latitude      = o["latitude"].asDouble,
                longitude     = o["longitude"].asDouble,
                complaintCount   = o["complaintCount"].asInt,
                resolvedCount    = o["resolvedCount"].asInt,
                budgetSanctioned = o["budgetSanctioned"].asLong,
                budgetSpent      = o["budgetSpent"].asLong,
                slaBreaches      = o["slaBreaches"].asInt
            )
        }
    }

    fun sortedBy(wards: List<WardAuthority>, field: WardSortField, ascending: Boolean): List<WardAuthority> {
        val sorted = when (field) {
            WardSortField.NAME            -> wards.sortedBy { it.wardName }
            WardSortField.COMPLAINTS      -> wards.sortedByDescending { it.complaintCount }
            WardSortField.RESOLUTION_RATE -> wards.sortedByDescending { it.resolutionRate }
            WardSortField.SLA_BREACHES    -> wards.sortedByDescending { it.slaBreaches }
            WardSortField.BUDGET_UTIL     -> wards.sortedByDescending { it.budgetUtilization }
        }
        return if (ascending) sorted.reversed() else sorted
    }

    private fun seedWards() = listOf(
        WardAuthority("W01","Berasia","Rajesh Sharma","+917552874100","North",23.282,77.355,142,128,8_500_000,7_200_000,14),
        WardAuthority("W02","Kothakhaira","Neha Verma","+917552874110","North",23.270,77.370,98,85,6_200_000,5_800_000,13),
        WardAuthority("W03","Chunar Ganj","Mohammed Khan","+917552874120","Central",23.255,77.385,156,132,7_200_000,6_500_000,24),
        WardAuthority("W04","Ayodhya Nagar","Suresh Patel","+917552874130","East",23.238,77.395,88,80,7_500_000,6_800_000,8),
        WardAuthority("W05","Govindpura","Anita Rajput","+917552874140","East",23.222,77.402,175,158,11_000_000,9_800_000,17),
        WardAuthority("W06","Indrapuri","Vikram Singh","+917552874150","East",23.215,77.415,112,95,6_800_000,6_200_000,17),
        WardAuthority("W07","Arera Colony","Priya Gupta","+917552874160","South",23.228,77.438,134,125,9_200_000,8_400_000,9),
        WardAuthority("W08","MP Nagar","Deepak Tiwari","+917552874170","Central",23.242,77.455,203,189,14_500_000,13_100_000,14),
        WardAuthority("W09","Shahpura","Kavita Dubey","+917552874180","South",23.235,77.468,167,151,10_800_000,9_500_000,16),
        WardAuthority("W10","Piplani","Arun Kumar","+917552874190","East",23.248,77.482,91,84,5_900_000,5_200_000,7),
        WardAuthority("W11","Bairagarh","Sunita Yadav","+917552874200","West",23.258,77.375,119,104,7_800_000,6_900_000,15),
        WardAuthority("W12","TT Nagar","Ramesh Mishra","+917552874210","Central",23.235,77.420,188,172,12_500_000,11_200_000,16),
        WardAuthority("W13","Kolar Road","Sheela Joshi","+917552874220","South",23.198,77.408,145,131,9_100_000,8_000_000,14)
    )
}

enum class WardSortField { NAME, COMPLAINTS, RESOLUTION_RATE, SLA_BREACHES, BUDGET_UTIL }
