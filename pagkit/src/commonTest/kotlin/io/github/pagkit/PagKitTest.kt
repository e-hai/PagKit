package io.github.pagkit

import io.github.pagkit.compose.PagState
import io.github.pagkit.model.PagLayerType
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource
import io.github.pagkit.model.PagTextData
import io.github.pagkit.model.PagTimeStretchMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagKitTest {

    @Test
    fun testScaleModeMapping() {
        assertEquals(PagScaleMode.None, PagScaleMode.fromNative(0))
        assertEquals(PagScaleMode.Stretch, PagScaleMode.fromNative(1))
        assertEquals(PagScaleMode.LetterBox, PagScaleMode.fromNative(2))
        assertEquals(PagScaleMode.Zoom, PagScaleMode.fromNative(3))
        assertEquals(PagScaleMode.LetterBox, PagScaleMode.fromNative(999)) // fallback
    }

    @Test
    fun testTimeStretchModeMapping() {
        assertEquals(PagTimeStretchMode.None, PagTimeStretchMode.fromNative(0))
        assertEquals(PagTimeStretchMode.Scale, PagTimeStretchMode.fromNative(1))
        assertEquals(PagTimeStretchMode.Repeat, PagTimeStretchMode.fromNative(2))
        assertEquals(PagTimeStretchMode.RepeatInverted, PagTimeStretchMode.fromNative(3))
        assertEquals(PagTimeStretchMode.Repeat, PagTimeStretchMode.fromNative(-1)) // fallback
    }

    @Test
    fun testLayerTypeMapping() {
        assertEquals(PagLayerType.Unknown, PagLayerType.fromNative(0))
        assertEquals(PagLayerType.Null, PagLayerType.fromNative(1))
        assertEquals(PagLayerType.Solid, PagLayerType.fromNative(2))
        assertEquals(PagLayerType.Text, PagLayerType.fromNative(3))
        assertEquals(PagLayerType.Shape, PagLayerType.fromNative(4))
        assertEquals(PagLayerType.Image, PagLayerType.fromNative(5))
        assertEquals(PagLayerType.PreCompose, PagLayerType.fromNative(6))
    }

    @Test
    fun testPagTextDataCreation() {
        val textData = PagTextData(
            text = "Hello CMP PagKit",
            fontSize = 32f,
            fillColorArgb = 0xFFFF0000.toInt(),
            fauxBold = true
        )
        assertEquals("Hello CMP PagKit", textData.text)
        assertEquals(32f, textData.fontSize)
        assertEquals(0xFFFF0000.toInt(), textData.fillColorArgb)
        assertTrue(textData.fauxBold)
        assertFalse(textData.fauxItalic)
    }

    @Test
    fun testPagStateLifecycleAndControls() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val source = PagSource.Bytes(bytes)
        val state = PagState(
            initialSource = source,
            initialAutoPlay = false,
            initialRepeatCount = 1,
            initialScaleMode = PagScaleMode.Zoom
        )

        assertEquals(source, state.source)
        assertFalse(state.isPlaying)
        assertEquals(1, state.repeatCount)
        assertEquals(PagScaleMode.Zoom, state.scaleMode)
        assertEquals(0.0, state.progress)
        assertNull(state.loadError)

        var playCalled = false
        var pauseCalled = false
        var stopCalled = false
        var seekProg: Double? = null
        var flushCalled = false

        state.playAction = { playCalled = true }
        state.pauseAction = { pauseCalled = true }
        state.stopAction = { stopCalled = true }
        state.seekAction = { seekProg = it }
        state.flushAction = { flushCalled = true }

        state.play()
        assertTrue(state.isPlaying)
        assertTrue(playCalled)

        state.pause()
        assertFalse(state.isPlaying)
        assertTrue(pauseCalled)

        state.seekTo(0.75)
        assertEquals(0.75, state.progress)
        assertEquals(0.75, seekProg)

        state.seekTo(1.5) // Clamped to 1.0
        assertEquals(1.0, state.progress)
        assertEquals(1.0, seekProg)

        state.seekTo(-0.5) // Clamped to 0.0
        assertEquals(0.0, state.progress)
        assertEquals(0.0, seekProg)

        state.seekTo(0.8)
        assertEquals(0.8, state.progress)
        state.stop()
        assertFalse(state.isPlaying)
        assertEquals(0.0, state.progress)
        assertTrue(stopCalled)

        state.flush()
        assertTrue(flushCalled)
    }

    @Test
    fun testPagSourceEquality() {
        val s1 = PagSource.Asset("anim.pag")
        val s2 = PagSource.Asset("anim.pag")
        val s3 = PagSource.Asset("other.pag")
        assertEquals(s1, s2)
        assertTrue(s1 != s3)

        val u1 = PagSource.Url("https://example.com/test.pag")
        val u2 = PagSource.Url("https://example.com/test.pag")
        assertEquals(u1, u2)

        val b1 = PagSource.Bytes(byteArrayOf(1, 2, 3))
        val b2 = PagSource.Bytes(byteArrayOf(1, 2, 3))
        assertEquals(b1, b2)
    }
}
