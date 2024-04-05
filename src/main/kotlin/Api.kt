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
            throw Exception("Failed to execute request response=$response")
        }

        val body = response.body?.string()

        log(body)

        return gson.fromJson(body, UniverseDto::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")
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
    fun travel(planetsToTravel: List<String>):PlanetInfo {
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
            throw Exception("Failed to execute request response=$response")
        }

        val body = response.body?.string()

        log(body)

        return gson.fromJson(body, PlanetInfo::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")
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

        log(body)

        return gson.fromJson(body, CollectResponse::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")
    }
}

class CollectResponse {
    @JvmField
    var garbage: Map<String, List<List<Int>>>? = null

    @JvmField
    var leaved: List<String>? = null
}

class PlanetInfo {
    @JvmField
    var fuelDiff: Int = 0

    @JvmField
    var planetDiffs: List<PlanetDiff>? = null

    @JvmField
    var planetGarbage: Map<String, List<List<Int>>>? = null

    @JvmField
    var shipGarbage: Map<String, List<List<Int>>>? = null
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