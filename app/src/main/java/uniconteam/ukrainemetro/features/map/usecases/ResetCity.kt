package uniconteam.ukrainemetro.features.map.usecases

import uniconteam.ukrainemetro.core.exceptions.Failure
import uniconteam.ukrainemetro.core.functionals.Either
import uniconteam.ukrainemetro.core.interactor.UseCase
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.features.map.MapFailure
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject

class ResetCity
@Inject constructor(private val prefsRepository: PrefsRepository) : UseCase<UseCase.None, UseCase.None>() {
    override suspend fun run(params: None): Either<Failure, None> {
        prefsRepository.cityId = null
        return Either.Right(None())
    }
}
