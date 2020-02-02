package eu.rtsketo.sakurastats.dbobjects

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ClanStats {
    @PrimaryKey
    var tag = ""
    var name = ""
    var state = ""
    var badge = ""
    var maxParticipants = 0
    var estimatedWins = 0
    var extraWins = 0.0
    var warTrophies = 0
    var actualWins = 0
    var remaining = 0
    var crowns = 0
    var clan1 = ""
    var clan2 = ""
    var clan3 = ""
    var clan4 = ""

}