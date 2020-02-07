package eu.rtsketo.sakurastats.dbobjects

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class PlayerStats {
    @PrimaryKey
    var tag = ""
    var name = ""
    var clan = ""
    var wins = 0
    var played = 0
    var curWins = 0
    var curPlay = 0
    var ratio = 0.0
    var norma = 0.0
    var current = false
    var missed = 0
    var cards = 0
    var wars = 0
    var chest = 0

}