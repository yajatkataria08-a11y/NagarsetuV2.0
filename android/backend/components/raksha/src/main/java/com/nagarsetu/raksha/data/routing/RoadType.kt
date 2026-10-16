package com.nagarsetu.raksha.data.routing

/**
 * Road type classifications used for route cost functions.
 * Ported from Raksha (com.safepath.indore.routing.RoadType).
 */
enum class RoadType(val penalty: Double) {
    MOTORWAY(0.0),
    PRIMARY(0.2),
    SECONDARY(0.5),
    TERTIARY(1.0),
    RESIDENTIAL(2.0),
    SERVICE(3.0)
}
