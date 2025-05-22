package team.unicon.ukrainemetro.repositories

import team.unicon.ukrainemetro.datasource.KharkivSubwayMapDataSource
import team.unicon.ukrainemetro.datasource.SubwayMapDataSource
import team.unicon.ukrainemetro.entities.Subway
import team.unicon.ukrainemetro.entities.SubwayInfo

interface SubwaysRepository {
    suspend fun getSubwayInfo(subway: Subway): SubwayInfo
}

class SubwaysRepositoryImpl(
    private val kharkivSubwayMapDataSource: KharkivSubwayMapDataSource
) : SubwaysRepository {
    private fun getSubwayDataSource(subway: Subway): SubwayMapDataSource {
        return when(subway) {
            Subway.Kharkiv -> kharkivSubwayMapDataSource
            else -> kharkivSubwayMapDataSource
        }
    }

    override suspend fun getSubwayInfo(subway: Subway): SubwayInfo {
        val dataSource = getSubwayDataSource(subway)

        return SubwayInfo(
            name = dataSource.name,
            elements = dataSource.elements
        )
    }
}