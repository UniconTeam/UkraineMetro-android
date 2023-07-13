package uniconteam.ukrainemetro.core.exceptions

sealed class Failure {
    object NetworkConnection : Failure()
    object ServerError : Failure()

    // Extend this class for feature specific failures
    abstract class FeatureFailure : Failure()
}
