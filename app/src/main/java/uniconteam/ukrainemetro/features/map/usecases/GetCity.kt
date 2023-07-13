package uniconteam.ukrainemetro.features.map.usecases

import uniconteam.ukrainemetro.core.exceptions.Failure
import uniconteam.ukrainemetro.core.functionals.Either
import uniconteam.ukrainemetro.core.functionals.Either.*
import uniconteam.ukrainemetro.core.interactor.UseCase
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.features.map.MapFailure
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject

class GetCity
@Inject constructor(private val mapsRepository: MapsRepository, private val prefsRepository: PrefsRepository) : UseCase<City, UseCase.None>() {
    override suspend fun run(params: None): Either<Failure, City> {
        if(prefsRepository.isHasCityId) {
            val cityId = prefsRepository.cityId!!
            val city = mapsRepository.fetchCityById(cityId)

            city?.let { return Right(it) }
        }

        return Left(MapFailure.CityNotSelected())
    }
}
