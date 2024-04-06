import java.io.File
import java.util.Date
import kotlin.math.roundToInt


var startCollectTs = 0L

fun main(args: Array<String>) {
    println("Started ${Date()}")
    apiToken = args.first()
    println(args.joinToString { it })



    while (true) {
        runCatching { doLoop() }
            .onFailure {
                System.err.println("doLoop failed " + it.stackTraceToString())
            }
    }
}

private fun doLoop() {

    val roundInfo = Api.getRoundInfo()

    val roundName = roundInfo.rounds?.firstOrNull { it.isCurrent }?.name ?: "unknown-round"

    // load visited planets from file from file system "$ROUND_NAME.txt"
    visitedPlanets.clear()
    val file = File("$roundName.txt")
    if (file.exists()) {
        file.readLines().forEach {
            visitedPlanets.add(it)
        }
    }
    visitedPlanets.add("Eden")
    visitedPlanets.add("Earth")


    while (true) {
        log("get universe")
        val universe = Api.getUniverse()

        SHIP_WIDTH = universe.ship!!.capacityX
        SHIP_HEIGHT = universe.ship!!.capacityY

        //log(universe)

        logMap("current cargo", universe.ship!!.garbage!!)

        val planetsToTravel = getPlanetsToTravel(universe).toMutableList()

        log("myPlanetIs=${universe.ship!!.planet!!.name!!}", "planetsToTravel", planetsToTravel)

        val finalPlanetsPath = planetsToTravel.also { it.removeFirst() }

        if (finalPlanetsPath.isEmpty()) {
            log("no planets to travel, sleep")
            Thread.sleep(1000)
            throw RuntimeException("no planets to travel, restart, wait for new round")
        }

        val planetInfo = Api.travel(finalPlanetsPath)

        saveVisitedPlanets(roundName)
        // log("planetInfo", planetInfo)

        if (planetInfo.planetGarbage!!.isEmpty()) {
            log("no garbage on planet, go next")
            Thread.sleep(500)
            continue
        }

        startCollectTs = System.currentTimeMillis()

        val maxGarbage = findBestLoadOut(universe, planetInfo)

        val occupied = maxGarbage.values.map { it.size }.sum()
        val maxOccupied = SHIP_HEIGHT * SHIP_WIDTH
        val ratio = occupied.toDouble() / maxOccupied
        val planetMaxOccupied = planetInfo.planetGarbage!!.values.map { it.size }.sum()
        logMap("maxGarbage occupied $occupied from $maxOccupied ratio=$ratio, planetMaxOccupied=$planetMaxOccupied", maxGarbage)

        if (maxOccupied > occupied) {
            log("there still garbage at='$planetsToTravel' removeIt from visited planets")
            visitedPlanets.remove(planetsToTravel.last())
        }
        saveVisitedPlanets(roundName)

        val collectResponse = Api.collect(maxGarbage)

        log("collected")

        Thread.sleep(500)
        // break
    }
}

fun saveVisitedPlanets(roundName: String) {
    val file = File("$roundName.txt")
    file.absoluteFile.let { println("file path " + it) }
    file.writeText(visitedPlanets.joinToString("\n"))
}

/**
 * output is like
 * . . . G
 * . . G G
 * . . G G
 *
 * size of ship is WIDTH HEIGHT from constants
 */
fun logMap(label: String, garbage: Map<String, List<List<Int>>>) {
    val ship = Array(SHIP_HEIGHT) { Array(SHIP_WIDTH) { '.' } }

    garbage.forEach { (garbageId, positions) ->
        positions.forEach {
            val x = it[0]
            val y = it[1]
            ship[y][x] = 'G'
        }
    }

    println(label)
    ship.forEach {
        println(it.joinToString(" "))
    }
}

fun findBestLoadOut(universe: UniverseDto, planetInfo: PlanetInfo): Map<String, List<List<Int>>> {
    val shipInitialOccupy = planetInfo.richShipGarbage.occupyArray

    val bestLoadout = mutableMapOf<String, List<List<Int>>>()

    val queue = ArrayDeque(planetInfo.richPlanetGarbage.listOfRichGarabge)

    var currentOccupy = shipInitialOccupy.copy()

    while (queue.isNotEmpty()) {
        val candidate = queue.removeFirst()

        val candidateOccupy = candidate.occupyArray

        var breakRepeat = false
        repeat(SHIP_WIDTH) { shiftX ->
            if (breakRepeat) {
                return@repeat
            }
            repeat(SHIP_HEIGHT) { shiftY ->
                if (breakRepeat) {
                    return@repeat
                }
                if (canBeApplied(currentOccupy, candidateOccupy, shiftX, shiftY)) {
                    currentOccupy = applyOccupy(currentOccupy, candidateOccupy, shiftX, shiftY)
                    val points: List<List<Int>> = planetInfo.richPlanetGarbage.simpleMap[candidate.garbageId]!!
                    bestLoadout[candidate.garbageId] = points.map {
                        listOf(it[0] + shiftX, it[1] + shiftY)
                    }
                    breakRepeat = true

                    logNotPretty("add to bestLoadout =${candidate.garbageId} shiftX=$shiftX shiftY=$shiftY cells=", bestLoadout)
                    return@repeat
                }
            }
        }
    }

    logMap("findBestLoadOut currentOccupy", currentOccupy)
    logNotPretty("findBestLoadOut took=${System.currentTimeMillis() - startCollectTs} bestLoadout", bestLoadout)

    return bestLoadout
}

fun logMap(label: String, garbage: BooleanPlainArray) {
    val ship = Array(garbage.cellsHeight) { Array(garbage.cellsWidth) { '.' } }

    garbage.fori { x, y, value ->
        ship[y][x] = if (value) 'G' else '.'
    }

    println(label)
    ship.forEach {
        println(it.joinToString(" "))
    }
}

fun canBeApplied(first: BooleanPlainArray, second: BooleanPlainArray, shiftX: Int, shiftY: Int): Boolean {
    var result = true

    second.fori { x, y, value ->
        if (value) {
            val x1 = x + shiftX
            val y1 = y + shiftY

            if (x1 < 0 || x1 >= first.cellsWidth || y1 < 0 || y1 >= first.cellsHeight) {
                result = false
                return@fori
            }

            if (first.getFast(x1, y1)) {
                result = false
                return@fori
            }
        }
    }

    return result
}

// apply second to first without copy, apply shift as well
fun applyOccupy(first: BooleanPlainArray, second: BooleanPlainArray, shiftX: Int, shiftY: Int): BooleanPlainArray {

    second.fori { x, y, value ->
        if (value) {
            val x1 = x + shiftX
            val y1 = y + shiftY

            first.setFast(x1, y1, true)
        }
    }

    return first

}

private val EDEN_PLANET = "Eden"

private fun getPlanetsToTravel(universe: UniverseDto): List<String> {

    val shipHasGarbage = universe.ship!!.garbage!!.isNotEmpty()

    val currentPlanet = universe.ship!!.planet

    val targetPlanet = if (shipHasGarbage) {
        log("getPlanetsToTravel go to empty garbage on Eden")
        EDEN_PLANET
    } else {

        findClosestPlanetWithConditionBfs(universe, currentPlanet!!.name!!).also {
            visitedPlanets.add(it)
        }.also {
            log("getPlanetsToTravel go to closest not visited planet with garbage $it")
        }
    }

    // find path to target planet by bfs
    val path = aStarSearch(universe, currentPlanet!!.name!!, targetPlanet)

    return path
}

val visitedPlanets = mutableSetOf<String>("Eden", "Earth")

fun findClosestPlanetWithConditionBfs(universe: UniverseDto, startPlanet: String): String {

    val graph = mutableMapOf<String, List<String>>()

    universe.universe!!.forEach {
        val from = it[0] as String
        val to = it[1] as String
        val cost = (it[2] as Double).roundToInt()

        graph[from] = graph.getOrDefault(from, listOf()) + to
        graph[to] = graph.getOrDefault(to, listOf()) + from
    }

    val visited = mutableSetOf<String>()
    val queue = mutableListOf(listOf(startPlanet))


    while (queue.isNotEmpty()) {
        val path = queue.removeAt(0)
        val node = path.last()

        if (node != startPlanet && !visitedPlanets.contains(node)) {
            return node
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

    return "Eden"
}

/**
 * take to account cost of move
 */
fun aStarSearch(universe: UniverseDto, currentPlanet: String, targetPlanet: String): List<String> {

    val graph = mutableMapOf<String, List<String>>()

    universe.universe!!.forEach {
        val from = it[0] as String
        val to = it[1] as String
        val cost = (it[2] as Double).roundToInt()

        graph[from] = graph.getOrDefault(from, listOf()) + to
        //   graph[to] = graph.getOrDefault(to, listOf()) + from
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

    return listOf()
}

fun log(vararg any: Any?) {
    any.joinToString {
        // to json
        when (it) {
            is String -> it
            else -> Api.gsonPretty.toJson(it ?: "null")
        }
    }.let {
        println(Date().toString() + ">" + it)
    }
}

fun logNotPretty(vararg any: Any?) {
    any.joinToString {
        // to json
        when (it) {
            is String -> it
            else -> Api.gson.toJson(it ?: "null")
        }
    }.let {
        println(Date().toString() + ">" + it)
    }
}

