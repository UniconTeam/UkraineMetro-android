package uniconteam.ukrainemetro.core.exceptions

sealed class Failure {
    object UnknownFailure : Failure()

    // Extend this class for feature specific failures
    abstract class FeatureFailure : Failure()
}
