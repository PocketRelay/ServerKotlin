package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.tdf.MapTdf
import com.jacobtread.kme.blaze.tdf.Tdf
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Game(
    val id: Int,
    val mid: Int,
    val host: PlayerSession,
) {

    class GameAttributes {
        private val values = HashMap<String, String>()
        private var isDirty = false
        private var tdfMap: MapTdf? = null
        private val lock = ReentrantReadWriteLock()

        fun setAttribute(key: String, value: String) {
            lock.write {
                isDirty = true
                values[key] = value
            }
        }

        fun setBulk(values: Map<String, String>) {
            lock.write {
                isDirty = true
                this.values.putAll(values)
            }
        }

        fun getMapTdf(): MapTdf {
            return lock.read {
                var value: MapTdf? = tdfMap
                if (value == null) {
                    isDirty = false
                    value = MapTdf("ATTR", Tdf.STRING, Tdf.STRING, values)
                    tdfMap = value
                }
                value
            }
        }
    }

    var gameState: Int = 0;
    var gameSetting: Int = 0;
    val attributes = GameAttributes()
    val players = ArrayList<PlayerSession>()

}