package eu.rtsketo.sakurastats.dbobjects

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class WarDay(@field:PrimaryKey var warDay: Long, var tag: String)