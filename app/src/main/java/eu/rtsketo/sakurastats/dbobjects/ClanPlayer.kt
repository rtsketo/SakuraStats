package eu.rtsketo.sakurastats.dbobjects

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ClanPlayer {
    @PrimaryKey
    var tag: String = null
    var clan: String? = null
    var last: Long = 0
    var score = 0
    var trophies = 0
    var smc = 0
    var legendary = 0
    var magical = 0
    var role: String? = null

}