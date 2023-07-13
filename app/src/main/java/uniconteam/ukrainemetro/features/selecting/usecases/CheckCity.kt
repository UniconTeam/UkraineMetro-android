package uniconteam.ukrainemetro.features.selecting.usecases

import uniconteam.ukrainemetro.core.exceptions.Failure
import uniconteam.ukrainemetro.core.functionals.Either
import uniconteam.ukrainemetro.core.interactor.UseCase
import uniconteam.ukrainemetro.core.repositories.PrefsRepository
import javax.inject.Inject

class CheckCity
@Inject constructor(private val prefsRepository: PrefsRepository) : UseCase<Boolean, UseCase.None>() {
    override suspend fun run(params: None): Either<Failure, Boolean> {
        return Either.Right(prefsRepository.isHasCityId)
        // cant: return Either.Left(Failure.UnknownFailure)
    }
}