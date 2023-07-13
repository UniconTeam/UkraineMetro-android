package uniconteam.ukrainemetro.features.selecting.usecases

import uniconteam.ukrainemetro.core.exceptions.Failure
import uniconteam.ukrainemetro.core.functionals.Either
import uniconteam.ukrainemetro.core.interactor.UseCase
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject

class GetCities
@Inject constructor(private val mapsRepository: MapsRepository, private val prefsRepository: PrefsRepository) : UseCase<List<City>, UseCase.None>() {
    override suspend fun run(params: None): Either<Failure, List<City>> {
        val cities = mapsRepository.fetchAllCities()
        return Either.Right(cities)
    }
}
