package io.github.pagkit.model

/**
 * 跨平台 PAG 时间轴标记点（用于识别关键动作点、事件点或循环边界）。
 *
 * @param startTimeUs 标记在图层时间轴上的起始时间（微秒）
 * @param durationUs 标记持续时间（微秒）
 * @param comment 标记名称或事件注释
 */
data class PagMarker(
    val startTimeUs: Long,
    val durationUs: Long,
    val comment: String
)
