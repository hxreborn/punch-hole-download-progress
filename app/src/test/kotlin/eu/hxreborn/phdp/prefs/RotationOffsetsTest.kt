package eu.hxreborn.phdp.prefs

import eu.hxreborn.phdp.FakePrefs
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationOffsetsTest {
    @Test
    fun `legacy four slot strings stay on the closed profile`() {
        val offsets = RotationOffsets.deserialize("1.0,2.0|3.0,4.0|5.0,6.0|7.0,8.0")
        assertEquals(OffsetPx(1f, 2f), offsets[FoldSlot.CLOSED, RotationSlot.R0])
        assertEquals(OffsetPx(7f, 8f), offsets[FoldSlot.CLOSED, RotationSlot.R270])
        assertEquals(OffsetPx(), offsets[FoldSlot.OPEN, RotationSlot.R0])
        assertEquals("1.0,2.0|3.0,4.0|5.0,6.0|7.0,8.0", offsets.serialize())
    }

    @Test
    fun `eight slot strings keep unfolded offsets`() {
        val offsets =
            RotationOffsets.deserialize(
                "1.0,2.0|3.0,4.0|5.0,6.0|7.0,8.0|9.0,10.0|11.0,12.0|13.0,14.0|15.0,16.0",
            )
        assertEquals(OffsetPx(1f, 2f), offsets[FoldSlot.CLOSED, RotationSlot.R0])
        assertEquals(OffsetPx(9f, 10f), offsets[FoldSlot.OPEN, RotationSlot.R0])
        assertEquals(OffsetPx(15f, 16f), offsets[FoldSlot.OPEN, RotationSlot.R270])
        assertEquals(
            "1.0,2.0|3.0,4.0|5.0,6.0|7.0,8.0|9.0,10.0|11.0,12.0|13.0,14.0|15.0,16.0",
            offsets.serialize(),
        )
    }

    @Test
    fun `writing an unfolded slot expands the serialized form`() {
        val updated =
            RotationOffsets.EMPTY.with(FoldSlot.OPEN, RotationSlot.R90, OffsetPx(4f, -5f))
        assertEquals(
            "0.0,0.0|0.0,0.0|0.0,0.0|0.0,0.0|0.0,0.0|4.0,-5.0|0.0,0.0|0.0,0.0",
            updated.serialize(),
        )
        assertEquals(OffsetPx(4f, -5f), updated[FoldSlot.OPEN, RotationSlot.R90])
        assertEquals(OffsetPx(), updated[FoldSlot.CLOSED, RotationSlot.R90])
    }

    @Test
    fun `malformed strings deserialize to empty`() {
        assertEquals(RotationOffsets.EMPTY, RotationOffsets.deserialize("1,2|3,4"))
        assertEquals(RotationOffsets.EMPTY, RotationOffsets.deserialize("nope"))
    }
}

class FoldScalesTest {
    @Test
    fun `legacy single pair applies to both postures`() {
        val scales = FoldScales.deserialize("0.65,0.7")
        assertEquals(ScaleXy(0.65f, 0.7f), scales[FoldSlot.CLOSED])
        assertEquals(ScaleXy(0.65f, 0.7f), scales[FoldSlot.OPEN])
        assertEquals("0.65,0.7", scales.serialize())
    }

    @Test
    fun `two pairs keep unfolded scale`() {
        val scales = FoldScales.deserialize("0.65,0.65|0.85,0.9")
        assertEquals(ScaleXy(0.65f, 0.65f), scales[FoldSlot.CLOSED])
        assertEquals(ScaleXy(0.85f, 0.9f), scales[FoldSlot.OPEN])
        assertEquals("0.65,0.65|0.85,0.9", scales.serialize())
    }

    @Test
    fun `legacy ring scale keys migrate into both slots`() {
        val prefs =
            FakePrefs(
                mapOf("ring_scale_x" to 0.65f, "ring_scale_y" to 0.7f),
            )
        val scales = Prefs.ringScales.read(prefs)
        assertEquals(ScaleXy(0.65f, 0.7f), scales[FoldSlot.CLOSED])
        assertEquals(ScaleXy(0.65f, 0.7f), scales[FoldSlot.OPEN])
    }
}

class FoldPostureResolverTest {
    @Test
    fun `non foldable devices stay on the closed slot`() {
        val posture =
            FoldPostureResolver.from(
                foldable = false,
                coverDisplay = false,
                hingeAngle = 180f,
            )
        assertEquals(FoldPosture.UNAVAILABLE, posture)
        assertEquals(FoldSlot.CLOSED, posture.toSlot())
    }

    @Test
    fun `hinge angle wins so tabletop is not treated as the cover`() {
        val posture =
            FoldPostureResolver.from(
                foldable = true,
                coverDisplay = true,
                hingeAngle = 90f,
            )
        assertEquals(FoldPosture.HALF_OPEN, posture)
        assertEquals(FoldSlot.OPEN, posture.toSlot())
    }

    @Test
    fun `cover display is used when the hinge has not reported yet`() {
        val posture =
            FoldPostureResolver.from(
                foldable = true,
                coverDisplay = true,
                hingeAngle = null,
            )
        assertEquals(FoldPosture.CLOSED, posture)
        assertEquals(FoldSlot.CLOSED, posture.toSlot())
    }

    @Test
    fun `hinge angle maps to folded half open and unfolded`() {
        assertEquals(FoldPosture.CLOSED, FoldPostureResolver.fromHingeAngle(0f))
        assertEquals(FoldPosture.HALF_OPEN, FoldPostureResolver.fromHingeAngle(90f))
        assertEquals(FoldPosture.OPEN, FoldPostureResolver.fromHingeAngle(180f))
        assertEquals(FoldSlot.OPEN, FoldPosture.HALF_OPEN.toSlot())
        assertEquals(FoldSlot.OPEN, FoldPosture.OPEN.toSlot())
    }

    @Test
    fun `inner display without a hinge reading uses the open slot`() {
        val posture =
            FoldPostureResolver.from(
                foldable = true,
                coverDisplay = false,
                hingeAngle = null,
            )
        assertEquals(FoldPosture.OPEN, posture)
        assertEquals(FoldSlot.OPEN, posture.toSlot())
    }
}
