package eu.hxreborn.phdp.ui.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.max

fun rotationSafeScale(polygons: List<RoundedPolygon>): Float {
    val bounds = FloatArray(4)
    val maxBounds = FloatArray(4)
    return polygons.minOf { polygon ->
        polygon.calculateBounds(bounds)
        polygon.calculateMaxBounds(maxBounds)
        max((bounds[2] - bounds[0]) / (maxBounds[2] - maxBounds[0]), (bounds[3] - bounds[1]) / (maxBounds[3] - maxBounds[1]))
    }
}

fun fitted(
    path: Path,
    size: Size,
    scaleFactor: Float,
    scaleMatrix: Matrix,
): Path {
    scaleMatrix.reset()
    scaleMatrix.scale(x = size.width * scaleFactor, y = size.height * scaleFactor)
    path.transform(scaleMatrix)
    path.translate(size.center - path.getBounds().center)
    return path
}
