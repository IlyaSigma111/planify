package com.planify.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.planify.app.data.Link
import com.planify.app.data.LinkNodeType
import com.planify.app.data.Repository
import com.planify.app.ui.theme.EdgeLine
import com.planify.app.ui.theme.NodeNote
import com.planify.app.ui.theme.NodeTask
import kotlin.math.sqrt

@Composable
fun GraphView(
    nodes: List<Repository.GraphNode>,
    links: List<Link>,
    connectionSource: Repository.GraphNode?,
    onNodeDrag: (Long, LinkNodeType, Float, Float) -> Unit,
    onNodeTap: (Repository.GraphNode) -> Unit,
    onNodeDoubleTap: (Repository.GraphNode) -> Unit,
    onConnectionEnd: (Repository.GraphNode) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var draggingNodeId by remember { mutableStateOf<Long?>(null) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.3f, 3f)
        offset += panChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .transformable(state = transformState)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes, connectionSource) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val worldTap = screenToWorld(tapOffset, offset, scale)
                            val tapped = findNodeAt(worldTap, nodes)
                            if (tapped != null) {
                                if (connectionSource != null) {
                                    onConnectionEnd(tapped)
                                } else {
                                    onNodeTap(tapped)
                                }
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            val worldTap = screenToWorld(tapOffset, offset, scale)
                            val tapped = findNodeAt(worldTap, nodes)
                            if (tapped != null) {
                                onNodeDoubleTap(tapped)
                            }
                        }
                    )
                }
                .pointerInput(nodes) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val worldStart = screenToWorld(startOffset, offset, scale)
                            val node = findNodeAt(worldStart, nodes)
                            draggingNodeId = node?.id
                        },
                        onDrag = { _, dragAmount ->
                            val nodeId = draggingNodeId ?: return@detectDragGestures
                            val node = nodes.find { it.id == nodeId } ?: return@detectDragGestures
                            val worldDrag = Offset(dragAmount.x / scale, dragAmount.y / scale)
                            onNodeDrag(
                                node.id, node.type,
                                node.posX + worldDrag.x,
                                node.posY + worldDrag.y
                            )
                        },
                        onDragEnd = { draggingNodeId = null },
                        onDragCancel = { draggingNodeId = null }
                    )
                }
        ) {
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(200, 220, 220, 220)
                textSize = 32f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }

            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, Offset.Zero)
            }) {
                for (link in links) {
                    val source = nodes.find { it.id == link.sourceId && it.type == link.sourceType }
                    val target = nodes.find { it.id == link.targetId && it.type == link.targetType }
                    if (source != null && target != null) {
                        drawCurvedEdge(
                            start = Offset(source.posX, source.posY),
                            end = Offset(target.posX, target.posY)
                        )
                    }
                }

                connectionSource?.let { src ->
                    drawCurvedEdge(
                        start = Offset(src.posX, src.posY),
                        end = Offset(src.posX + 200f, src.posY + 100f),
                        isDashed = true
                    )
                }

                for (node in nodes) {
                    val isHighlighted = connectionSource?.id == node.id && connectionSource?.type == node.type
                    val color = when (node.type) {
                        LinkNodeType.NOTE -> NodeNote
                        LinkNodeType.TASK -> NodeTask
                    }
                    drawNode(
                        center = Offset(node.posX, node.posY),
                        color = color,
                        label = node.title,
                        textPaint = textPaint,
                        isHighlighted = isHighlighted
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNode(
    center: Offset,
    color: Color,
    label: String,
    textPaint: Paint,
    isHighlighted: Boolean
) {
    val radius = 44f

    if (isHighlighted) {
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = radius + 14f,
            center = center
        )
    }

    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    drawCircle(
        color = color.copy(alpha = 0.15f),
        radius = radius - 3f,
        center = center
    )

    val truncated = if (label.length > 14) label.take(14) + ".." else label
    val fontMetrics = textPaint.fontMetrics
    val textY = center.y - (fontMetrics.ascent + fontMetrics.descent) / 2f
    drawContext.canvas.nativeCanvas.drawText(
        truncated,
        center.x,
        textY,
        textPaint
    )
}

private fun DrawScope.drawCurvedEdge(
    start: Offset,
    end: Offset,
    isDashed: Boolean = false
) {
    val controlOffset = 40f
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + controlOffset, start.y + controlOffset,
            end.x - controlOffset, end.y - controlOffset,
            end.x, end.y
        )
    }

    drawPath(
        path = path,
        color = EdgeLine.copy(alpha = if (isDashed) 0.4f else 0.6f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
}

private fun screenToWorld(screen: Offset, offset: Offset, scale: Float): Offset {
    return Offset(
        (screen.x - offset.x) / scale,
        (screen.y - offset.y) / scale
    )
}

private fun findNodeAt(worldPos: Offset, nodes: List<Repository.GraphNode>): Repository.GraphNode? {
    val hitRadius = 60f
    return nodes.minByOrNull { n ->
        val dx = worldPos.x - n.posX
        val dy = worldPos.y - n.posY
        dx * dx + dy * dy
    }?.let { n ->
        val dx = worldPos.x - n.posX
        val dy = worldPos.y - n.posY
        if (dx * dx + dy * dy <= hitRadius * hitRadius) n else null
    }
}
