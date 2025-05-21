package team.unicon.ukrainemetro

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform