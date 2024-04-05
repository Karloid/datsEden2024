import java.util.Date
import kotlin.math.roundToInt

fun main(args: Array<String>) {
    println("Started ${Date()}")
    apiToken = args.first()
    println(args.joinToString { it })


    while (true) {
        val universe = Api.getUniverse()

        log(universe)

        val planetsToTravel = getPlanetsToTravel(universe).toMutableList().also { it.removeFirst() }

        log("planetsToTravel", planetsToTravel)

        val planetInfo = Api.travel(planetsToTravel)

        log("planetInfo", planetInfo)

        if (planetInfo.planetGarbage!!.isEmpty()) {
            Thread.sleep(500)
            continue
        }

        val maxGarbage = planetInfo.planetGarbage!!.entries!!.maxBy { it.value.size }
        val collectResponse = Api.collect(mapOf(maxGarbage.key to maxGarbage.value))

        Thread.sleep(500)
        break
    }

}

private val EDEN_PLANET = "Eden"

private fun getPlanetsToTravel(universe: UniverseDto): List<String> {

    val shipHasGarbage = universe.ship!!.garbage!!.isNotEmpty()

    val currentPlanet = universe.ship!!.planet

    val targetPlanet = if (shipHasGarbage) {
        EDEN_PLANET
    } else {
        universe.universe!!.filter {
            val planetFrom = it[0]
            planetFrom == currentPlanet!!.name
        }
            .minBy {
                val travelCost = (it[2] as Double).roundToInt()
                travelCost
            }[1] as String
    }


    // find path to target planet by bfs
    val path = bfs(universe, currentPlanet!!.name!!, targetPlanet)

    return path
}

fun bfs(universe: UniverseDto, currentPlanet: String, targetPlanet: String): List<String> {
    // take to account cost to travel
    val graph = mutableMapOf<String, List<String>>()

    universe.universe!!.forEach {
        val from = it[0] as String
        val to = it[1] as String
        val cost = (it[2] as Double).roundToInt()

        graph[from] = graph.getOrDefault(from, listOf()) + to
        graph[to] = graph.getOrDefault(to, listOf()) + from
    }

    val visited = mutableSetOf<String>()
    val queue = mutableListOf(listOf(currentPlanet))

    while (queue.isNotEmpty()) {
        val path = queue.removeAt(0)
        val node = path.last()

        if (node == targetPlanet) {
            return path
        }

        if (visited.contains(node)) {
            continue
        }

        visited.add(node)

        graph[node]?.forEach {
            if (!visited.contains(it)) {
                queue.add(path + it)
            }
        }
    }

    return emptyList()
}

fun log(vararg any: Any?) {
    any.joinToString {
        // to json
        Api.gsonPretty.toJson(it ?: "null")
    }.let {
        println(Date().toString() + ">" + it)
    }
}
