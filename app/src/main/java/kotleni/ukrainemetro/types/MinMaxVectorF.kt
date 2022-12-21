package kotleni.ukrainemetro.types

/** Minimal and maximal float vectors
 * @param minVector Minimal vector
 * @param maxVector Maximal vector
 */
data class MinMaxVectorF(
    val minVector: VectorF,
    val maxVector: VectorF
)