package team.unicon.ukrainemetro.entities

import androidx.compose.ui.geometry.Offset

data class Vector(val x: Int, val y: Int)

fun Vector.toOffset(): Offset = Offset(x.toFloat(), y.toFloat())