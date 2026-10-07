package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.gaminghub.musify.desktop.model.TrackModel
import com.gaminghub.musify.desktop.ui.theme.*
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

/**
 * Custom dark Windows caption/title bar:
 * - 100% dark background (#0D0D12) seamlessly matching Musify's dark theme
 * - "Musify" branding in punchy Musify Emerald Green (#1DB954)
 * - Dynamic currently playing song title and artist
 * - Full dragging via WindowDraggableArea and double-click to Maximize/Restore
 * - Native-feel Minimize, Maximize/Restore, and Close action buttons
 */
@Composable
fun WindowScope.DesktopCustomTitleBar(
    windowState: WindowState,
    currentTrack: TrackModel?,
    isPlaying: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    WindowDraggableArea(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFF0D0D12))
            .pointerInput(isMaximized) {
                detectTapGestures(
                    onDoubleTap = {
                        windowState.placement = if (isMaximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Brand Logo & Title in Musify Emerald Green
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Musify Logo",
                    tint = MusifyEmerald,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Musify",
                    color = MusifyEmerald,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Separator dot
            Text(
                text = "•",
                color = TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Current track or App subtitle
            if (currentTrack != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = currentTrack.title,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = " — ${currentTrack.artist}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = "Windows Music Player",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
                )
            }

            // Draggable flexible space in the center
            Spacer(modifier = Modifier.weight(1f))

            // Window Action Buttons (Minimize, Maximize/Restore, Close)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Minimize
                WindowControlButton(
                    onClick = { windowState.isMinimized = true },
                    contentDescription = "Minimize"
                ) {
                    Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = "Minimize",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp).offset(y = (-4).dp)
                    )
                }

                // Maximize / Restore
                WindowControlButton(
                    onClick = {
                        windowState.placement = if (isMaximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    contentDescription = if (isMaximized) "Restore" else "Maximize"
                ) {
                    Icon(
                        imageVector = if (isMaximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                        contentDescription = if (isMaximized) "Restore" else "Maximize",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Close (red on hover like native Windows)
                WindowControlButton(
                    onClick = onClose,
                    isClose = true,
                    contentDescription = "Close"
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    isClose: Boolean = false,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isHovered && isClose -> Color(0xFFE53935) // Native Windows Close Red
        isHovered -> Color(0xFF282832) // Soft highlight for Min/Max
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .width(42.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Attaches border resizing listeners to an undecorated AWT/Swing Window
 * so the window can be resized by dragging any edge or corner.
 */
fun attachWindowResizer(window: java.awt.Window, isMaximized: () -> Boolean) {
    val borderThickness = 6
    var resizeDirection = Cursor.DEFAULT_CURSOR
    var dragStartPoint: Point? = null
    var startBounds: java.awt.Rectangle? = null

    val motionListener = object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            if (isMaximized()) {
                window.cursor = Cursor.getDefaultCursor()
                return
            }

            val p = e.point
            val w = window.width
            val h = window.height

            val onLeft = p.x in 0..borderThickness
            val onRight = p.x in (w - borderThickness)..w
            val onTop = p.y in 0..borderThickness
            val onBottom = p.y in (h - borderThickness)..h

            resizeDirection = when {
                onTop && onLeft -> Cursor.NW_RESIZE_CURSOR
                onTop && onRight -> Cursor.NE_RESIZE_CURSOR
                onBottom && onLeft -> Cursor.SW_RESIZE_CURSOR
                onBottom && onRight -> Cursor.SE_RESIZE_CURSOR
                onLeft -> Cursor.W_RESIZE_CURSOR
                onRight -> Cursor.E_RESIZE_CURSOR
                onTop -> Cursor.N_RESIZE_CURSOR
                onBottom -> Cursor.S_RESIZE_CURSOR
                else -> Cursor.DEFAULT_CURSOR
            }

            window.cursor = Cursor.getPredefinedCursor(resizeDirection)
        }

        override fun mouseDragged(e: MouseEvent) {
            if (isMaximized() || resizeDirection == Cursor.DEFAULT_CURSOR) return
            val start = dragStartPoint ?: return
            val bounds = startBounds ?: return

            val dx = e.xOnScreen - start.x
            val dy = e.yOnScreen - start.y

            var newX = bounds.x
            var newY = bounds.y
            var newW = bounds.width
            var newH = bounds.height

            val minW = 400
            val minH = 300

            when (resizeDirection) {
                Cursor.E_RESIZE_CURSOR -> {
                    newW = (bounds.width + dx).coerceAtLeast(minW)
                }
                Cursor.S_RESIZE_CURSOR -> {
                    newH = (bounds.height + dy).coerceAtLeast(minH)
                }
                Cursor.SE_RESIZE_CURSOR -> {
                    newW = (bounds.width + dx).coerceAtLeast(minW)
                    newH = (bounds.height + dy).coerceAtLeast(minH)
                }
                Cursor.W_RESIZE_CURSOR -> {
                    val potentialW = bounds.width - dx
                    if (potentialW >= minW) {
                        newX = bounds.x + dx
                        newW = potentialW
                    }
                }
                Cursor.N_RESIZE_CURSOR -> {
                    val potentialH = bounds.height - dy
                    if (potentialH >= minH) {
                        newY = bounds.y + dy
                        newH = potentialH
                    }
                }
                Cursor.NW_RESIZE_CURSOR -> {
                    val potentialW = bounds.width - dx
                    if (potentialW >= minW) {
                        newX = bounds.x + dx
                        newW = potentialW
                    }
                    val potentialH = bounds.height - dy
                    if (potentialH >= minH) {
                        newY = bounds.y + dy
                        newH = potentialH
                    }
                }
                Cursor.NE_RESIZE_CURSOR -> {
                    newW = (bounds.width + dx).coerceAtLeast(minW)
                    val potentialH = bounds.height - dy
                    if (potentialH >= minH) {
                        newY = bounds.y + dy
                        newH = potentialH
                    }
                }
                Cursor.SW_RESIZE_CURSOR -> {
                    val potentialW = bounds.width - dx
                    if (potentialW >= minW) {
                        newX = bounds.x + dx
                        newW = potentialW
                    }
                    newH = (bounds.height + dy).coerceAtLeast(minH)
                }
            }

            window.setBounds(newX, newY, newW, newH)
        }
    }

    val mouseListener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            if (!isMaximized() && resizeDirection != Cursor.DEFAULT_CURSOR) {
                dragStartPoint = Point(e.xOnScreen, e.yOnScreen)
                startBounds = window.bounds
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            dragStartPoint = null
            startBounds = null
        }

        override fun mouseExited(e: MouseEvent) {
            if (dragStartPoint == null) {
                window.cursor = Cursor.getDefaultCursor()
            }
        }
    }

    window.addMouseListener(mouseListener)
    window.addMouseMotionListener(motionListener)
}
