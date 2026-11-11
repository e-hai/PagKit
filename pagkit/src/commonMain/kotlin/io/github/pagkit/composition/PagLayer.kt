package io.github.pagkit.composition

import io.github.pagkit.model.PagLayerType
import io.github.pagkit.model.PagMarker

/**
 * 跨平台 PAG 基础图层封装。
 */
expect open class PagLayer {
    /** 图层类型 */
    val layerType: PagLayerType

    /** 图层名称 */
    val layerName: String

    /** 图层时间轴标记点列表 */
    val markers: List<PagMarker>

    /** 可编辑索引（若不可编辑则为 -1） */
    val editableIndex: Int

    /** 图层是否可见 */
    var visible: Boolean

    /** 图层透明度 (0.0f - 1.0f) */
    var alpha: Float

    /** 图层时长（微秒） */
    val durationUs: Long

    /** 图层帧率 */
    val frameRate: Float

    /** 图层在父合成时间轴中的起始时间（微秒） */
    var startTimeUs: Long

    /** 图层当前播放时间（微秒） */
    var currentTimeUs: Long

    /** 图层播放进度 (0.0 - 1.0) */
    var progress: Double

    /** 是否从父合成的时间轴中排除 */
    var excludedFromTimeline: Boolean

    /** 重置变换矩阵 */
    fun resetMatrix()

    /** 设置平移变换 */
    fun setTranslation(dx: Float, dy: Float)

    /** 设置缩放变换 */
    fun setScale(sx: Float, sy: Float)

    /** 设置旋转角度（角度制，例如 45f） */
    fun setRotation(degrees: Float)
}
