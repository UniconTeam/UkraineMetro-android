package uniconteam.ukrainemetro.features.selecting.usecases

import uniconteam.ukrainemetro.core.exceptions.Failure
import uniconteam.ukrainemetro.core.functionals.Either
import uniconteam.ukrainemetro.core.interactor.UseCase
import uniconteam.ukrainemetro.core.repositories.MapsRepository
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import uniconteam.ukrainemetro.features.map.MapFailure
import uniconteam.ukrainemetro.mapview.entities.City
import javax.inject.Inject

class UpdateCity
@Inject constructor(private val prefsRepository: PrefsRepository) : UseCase<UseCase.None, UpdateCity.Params>() {
    override suspend fun run(params: Params): Either<Failure, UseCase.None> {
        prefsRepository.cityId = params.cityId
        return Either.Right(None())
        // cant: return Either.Left(Failure.UnknownFailure)
    }

    data class Params(val cityId: String)
}
