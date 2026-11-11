package io.github.pagkit.composition

/**
 * 跨平台 PAG 离线序列帧解码器。
 * 支持从 [PagComposition] 逐帧解码渲染为位图或直接读取像素。
 */
expect class PagDecoder {
    val width: Int
    val height: Int
    val numFrames: Int
    val frameRate: Float

    /** 检查指定帧自上次读取后是否发生变化 */
    fun checkFrameChanged(index: Int): Boolean

    companion object {
        fun make(composition: PagComposition): PagDecoder?
        fun make(composition: PagComposition, maxFrameRate: Float, scale: Float): PagDecoder?
    }
}
