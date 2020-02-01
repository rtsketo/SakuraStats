package eu.rtsketo.sakurastats.dbobjects

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ClanStats {
    @PrimaryKey
    var tag: String = null
    var name: String = null
    var state: String = null
    var badge: String = null
    var maxParticipants = 0
    var estimatedWins = 0
    var extraWins = 0.0
    var warTrophies = 0
    var actualWins = 0
    var remaining = 0
    var crowns = 0
    var clan1: String = null
    var clan2: String = null
    var clan3: String = null
    var clan4: String = null

}