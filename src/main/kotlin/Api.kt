import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient

var apiToken = ""

object Api {

    val gson = Gson()

    // pretty print gson
    val gsonPretty = GsonBuilder().setPrettyPrinting().create()

    val okHttpClient = OkHttpClient()


    /**
     *
     * https://datsedenspace.datsteam.dev/player/universe
     *
     * Content type
     * application/json
     *
     * response example
     *
     * {
     *   "name": "MyTeam",
     *   "ship": {
     *     "capacityX": 8,
     *     "capacityY": 11,
     *     "fuelUsed": 1000,
     *     "garbage": {
     *       "6fTzQid": [
     *         [
     *           0,
     *           0
     *         ],
     *         [
     *           0,
     *           1
     *         ],
     *         [
     *           1,
     *           1
     *         ]
     *       ],
     *       "RVnTkM59": [
     *         [
     *           2,
     *           2
     *         ],
     *         [
     *           2,
     *           3
     *         ],
     *         [
     *           3,
     *           3
     *         ],
     *         [
     *           5,
     *           3
     *         ],
     *         [
     *           3,
     *           5
     *         ]
     *       ]
     *     },
     *     "planet": {
     *       "garbage": {
     *         "6fTzQid": [
     *           [
     *             0,
     *             0
     *           ],
     *           [
     *             0,
     *             1
     *           ],
     *           [
     *             1,
     *             1
     *           ]
     *         ],
     *         "RVnTkM59": [
     *           [
     *             0,
     *             0
     *           ],
     *           [
     *             0,
     *             1
     *           ],
     *           [
     *             1,
     *             1
     *           ],
     *           [
     *             2,
     *             1
     *           ],
     *           [
     *             1,
     *             2
     *           ]
     *         ]
     *       },
     *       "name": "string"
     *     }
     *   },
     *   "universe": [
     *     [
     *       "Earth",
     *       "Reinger",
     *       100
     *     ],
     *     [
     *       "Reinger",
     *       "Earth",
     *       100
     *     ],
     *     [
     *       "Reinger",
     *       "Larkin",
     *       100
     *     ]
     *   ]
     * }
     */
    fun getUniverse(): UniverseDto {
        throttle()
        // implementation
        val request = okhttp3.Request.Builder()
            .url("https://datsedenspace.datsteam.dev/player/universe")
            .get()
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Auth-Token", apiToken)
            .build()

        val call = okHttpClient.newCall(request)

        val response = call.execute()

        if (response.isSuccessful.not()) {
            throw Exception("Failed to execute request response=$response body=${response.body?.string()}")
        }

        val body = response.body?.string()

        log(body)

        val result = gson.fromJson(body, UniverseDto::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")
        return result
    }


    val lastRequest = ArrayDeque<Long>()

    /**
     * we should not fire more than 4 request per second
     */
    private fun throttle() {
        val currentTs = System.currentTimeMillis()

        // remove requests older than 1 second

        while (lastRequest.isNotEmpty() && currentTs - lastRequest.first() > 1000) {
            lastRequest.removeFirst()
        }

        if (lastRequest.size < 4) {
            lastRequest.add(currentTs)
            return
        }

        val sleepTime = 1000 - (currentTs - lastRequest.first())

        log("throttle sleep for $sleepTime")
        Thread.sleep(sleepTime)

        lastRequest.add(System.currentTimeMillis())
    }

    /**
     * request example
     * {
     * "planets": [
     * "Reinger-77",
     * "Earth"
     * ]
     * }
     *
     * response example
     *
     * {
     *   "fuelDiff": 1000,
     *   "planetDiffs": [
     *     {
     *       "from": "Earth",
     *       "fuel": 100,
     *       "to": "Reinger"
     *     }
     *   ],
     *   "planetGarbage": {
     *     "6fTzQid": [
     *       [
     *         0,
     *         0
     *       ],
     *       [
     *         0,
     *         1
     *       ],
     *       [
     *         1,
     *         1
     *       ]
     *     ],
     *     "RVnTkM59": [
     *       [
     *         0,
     *         0
     *       ],
     *       [
     *         0,
     *         1
     *       ],
     *       [
     *         1,
     *         1
     *       ],
     *       [
     *         2,
     *         1
     *       ],
     *       [
     *         1,
     *         2
     *       ]
     *     ]
     *   },
     *   "shipGarbage": {
     *     "71B2XMi": [
     *       [
     *         2,
     *         10
     *       ],
     *       [
     *         2,
     *         9
     *       ],
     *       [
     *         2,
     *         8
     *       ],
     *       [
     *         3,
     *         8
     *       ]
     *     ]
     *   }
     * }
     */
    fun travel(planetsToTravel: List<String>): PlanetInfo {
        throttle()
        // implementation
        val requestBody = gson.toJson(mapOf("planets" to planetsToTravel))
        val request = okhttp3.Request.Builder()
            .url("https://datsedenspace.datsteam.dev/player/travel")
            .post(okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Auth-Token", apiToken)
            .build()

        val call = okHttpClient.newCall(request)

        val response = call.execute()

        if (response.isSuccessful.not()) {
            throw Exception("Failed to execute request response=$response body=${response.body?.string()}")
        }

        val body = response.body?.string()

        log(body)

        val planetInfo = gson.fromJson(body, PlanetInfo::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")

        planetInfo.constructGarbageArrays()
        return planetInfo
    }

    /**
     *
     * https://datsedenspace.datsteam.dev/player/collect
     *
     * request
     *
     * {
     *   "garbage": {
     *     "71B2XMi": [
     *       [
     *         2,
     *         10
     *       ],
     *       [
     *         2,
     *         9
     *       ],
     *       [
     *         2,
     *         8
     *       ],
     *       [
     *         3,
     *         8
     *       ]
     *     ]
     *   }
     * }
     *
     * response
     * {
     *   "garbage": {
     *     "71B2XMi": [
     *       [
     *         2,
     *         10
     *       ],
     *       [
     *         2,
     *         9
     *       ],
     *       [
     *         2,
     *         8
     *       ],
     *       [
     *         3,
     *         8
     *       ]
     *     ]
     *   },
     *   "leaved": [
     *     "RWEaughM",
     *     "90B2XMi"
     *   ]
     * }
     */
    fun collect(garbageToCollect: Map<String, List<List<Int>>>): CollectResponse {
        throttle()

        val requestBody = gson.toJson(mapOf("garbage" to garbageToCollect))
        val request = okhttp3.Request.Builder()
            .url("https://datsedenspace.datsteam.dev/player/collect")
            .post(okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Auth-Token", apiToken)
            .build()

        val call = okHttpClient.newCall(request)

        val response = call.execute()

        if (response.isSuccessful.not()) {
            throw Exception("Failed to execute request response=$response ${response.body?.string()} request=$requestBody")
        }

        val body = response.body?.string()

        //  log(body)

        return gson.fromJson(body, CollectResponse::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")
    }

    /**
     *
     * GET https://datsedenspace.datsteam.dev/player/rounds
     *
     * response
     * {
     *   "rounds": [
     *     {
     *       "startAt": "2024-04-04 14:00:00",
     *       "endAt": "2024-04-04 14:30:00",
     *       "isCurrent": false,
     *       "name": "round 1",
     *       "planetCount": 100
     *     }
     *   ]
     * }
     */
    fun getRoundInfo(): RoundsInfo {
        throttle()

        val request = okhttp3.Request.Builder()
            .url("https://datsedenspace.datsteam.dev/player/rounds")
            .get()
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Auth-Token", apiToken)
            .build()

        val call = okHttpClient.newCall(request)

        val response = call.execute()

        if (response.isSuccessful.not()) {
            throw Exception("Failed to execute request response=$response ${response.body?.string()}")
        }

        val body = response.body?.string()

        log(body)

        return gson.fromJson(body, RoundsInfo::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")
    }

    /**
     * DELETE https://datsedenspace.datsteam.dev/player/reset
     */
    fun resetRound() {
        throttle()

        val request = okhttp3.Request.Builder()
            .url("https://datsedenspace.datsteam.dev/player/reset")
            .delete()
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Auth-Token", apiToken)
            .build()

        val call = okHttpClient.newCall(request)

        val response = call.execute()

        if (response.isSuccessful.not()) {
            throw Exception("Failed to execute request response=$response ${response.body?.string()}")
        }

        val body = response.body?.string()

        log(body)
    }
}


class RoundsInfo {
    @JvmField
    var rounds: List<Round>? = null


}

class Round {
    @JvmField
    var startAt: String? = null

    @JvmField
    var endAt: String? = null

    @JvmField
    var isCurrent: Boolean = false

    @JvmField
    var name: String = ""

    @JvmField
    var planetCount: Int = 0

}

class CollectResponse {
    @JvmField
    var garbage: Map<String, List<List<Int>>>? = null

    @JvmField
    var leaved: List<String>? = null
}

var SHIP_WIDTH = 5
var SHIP_HEIGHT = 7

class PlanetInfo {
    @JvmField
    var fuelDiff: Int = 0

    @JvmField
    var planetDiffs: List<PlanetDiff>? = null

    @JvmField
    var planetGarbage: Map<String, List<List<Int>>>? = null

    @JvmField
    var shipGarbage: Map<String, List<List<Int>>>? = null

    lateinit var richShipGarbage: RichShipGarbage
    lateinit var richPlanetGarbage: RichPlanetGarbage


    fun constructGarbageArrays() {
        richShipGarbage = RichShipGarbage()

        richShipGarbage.simpleMap = shipGarbage ?: emptyMap()

        richShipGarbage.occupyArray = BooleanPlainArray(SHIP_WIDTH, SHIP_HEIGHT)

        richShipGarbage.simpleMap.values.forEach {
            it.forEach { pair ->
                richShipGarbage.occupyArray.set(pair[0], pair[1], true)
            }
        }


        richPlanetGarbage = RichPlanetGarbage()
        richPlanetGarbage.simpleMap = planetGarbage!!
        richPlanetGarbage.listOfRichGarabge = richPlanetGarbage.simpleMap.entries.map { entry ->
            RichGarbage(entry.key, entry.value)
        }
    }
}

class RichGarbage(val garbageId: String, value: List<List<Int>>) {

    val occupyArray = BooleanPlainArray(SHIP_WIDTH, SHIP_HEIGHT)

    init {
        value.forEach { pair ->
            occupyArray.set(pair[0], pair[1], true)
        }

        //val listOfArrayPoints: List<List<Int>> = occupyArray.toListOfArrayPoints()
        // TODO add rotation for occupyArray
    }

}

class RichShipGarbage {


    lateinit var occupyArray: BooleanPlainArray
    lateinit var simpleMap: Map<String, List<List<Int>>>
}

class RichPlanetGarbage {
    lateinit var simpleMap: Map<String, List<List<Int>>>
    lateinit var listOfRichGarabge: List<RichGarbage>
}


class PlanetDiff {
    @JvmField
    var from: String? = null

    @JvmField
    var fuel: Int = 0

    @JvmField
    var to: String? = null
}

class UniverseDto {
    @JvmField
    var name: String? = null

    @JvmField
    var ship: ShipDto? = null

    @JvmField
    var universe: List<List<Any>>? = null
}

class ShipDto {
    @JvmField
    var capacityX: Int = 0

    @JvmField
    var capacityY: Int = 0

    @JvmField
    var fuelUsed: Int = 0

    @JvmField
    var garbage: Map<String, List<List<Int>>>? = null

    @JvmField
    var planet: PlanetDto? = null
}

class PlanetDto {
    @JvmField
    var garbage: Map<String, List<List<Int>>>? = null

    @JvmField
    var name: String? = null
}