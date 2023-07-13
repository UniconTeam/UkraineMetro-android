package uniconteam.ukrainemetro.mapview.view

class ScaleListener(var onUpdateScale: (factor: Float) -> Unit) : MyScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScale(detector: MyScaleGestureDetector?): Boolean {
        detector?.getScaleFactor()?.let {
            onUpdateScale.invoke(it)
        }

        return super.onScale(detector)
    }
}