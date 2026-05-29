package ru.mrdire.chatselect

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.text.OrderedText
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ChatTextSelection {
    data class VisibleLine(
        val id: LineId,
        val text: String
    )

    data class LineId(
        val identityHash: Int,
        val text: String
    )

    private data class Pos(
        val lineId: LineId,
        val char: Int
    )

    private var pending = false
    private var selecting = false

    private var start: Pos? = null
    private var end: Pos? = null

    private var selectedSnapshotText: String = ""

    fun clear() {
        pending = false
        selecting = false
        start = null
        end = null
        selectedSnapshotText = ""
    }

    fun prepare(lines: List<VisibleLine>, lineIndex: Int, char: Int) {
        val line = lines.getOrNull(lineIndex) ?: return

        pending = true
        selecting = false

        start = Pos(line.id, char)
        end = Pos(line.id, char)

        selectedSnapshotText = ""
    }

    fun dragIfMoved(
        lines: List<VisibleLine>,
        lineIndex: Int,
        char: Int
    ) {
        val startPos = start ?: return
        val line = lines.getOrNull(lineIndex) ?: return

        if (pending) {
            val sameLine = line.id.identityHash == startPos.lineId.identityHash
            val movedEnough = !sameLine || abs(char - startPos.char) >= 2

            if (!movedEnough) return

            pending = false
            selecting = true
        }

        if (!selecting) return

        end = Pos(line.id, char)
        selectedSnapshotText = getSelectedText(lines)
    }

    fun finish() {
        pending = false
        selecting = false
    }

    fun hasSelection(): Boolean {
        return selectedSnapshotText.isNotBlank()
    }

    fun copyToClipboard(client: MinecraftClient) {
        if (selectedSnapshotText.isNotBlank()) {
            client.keyboard.clipboard = selectedSnapshotText
        }
    }

    fun selectWord(
        lines: List<VisibleLine>,
        lineIndex: Int,
        charIndex: Int
    ) {
        val visibleLine = lines.getOrNull(lineIndex) ?: return
        val line = visibleLine.text

        if (line.isEmpty()) return

        val safeIndex = charIndex.coerceIn(0, line.length - 1)

        if (line[safeIndex].isWhitespace()) return

        var startIndex = safeIndex
        var endIndex = safeIndex

        while (startIndex > 0 && !line[startIndex - 1].isWhitespace()) {
            startIndex--
        }

        while (endIndex < line.length - 1 && !line[endIndex + 1].isWhitespace()) {
            endIndex++
        }

        start = Pos(visibleLine.id, startIndex)
        end = Pos(visibleLine.id, endIndex + 1)

        pending = false
        selecting = false

        selectedSnapshotText = line.substring(startIndex, endIndex + 1)
    }

    private fun resolvePos(pos: Pos, lines: List<VisibleLine>): Pair<Int, Int>? {
        val byIdentity = lines.indexOfFirst {
            it.id.identityHash == pos.lineId.identityHash
        }

        val index = if (byIdentity != -1) {
            byIdentity
        } else {
            lines.indexOfFirst {
                it.text == pos.lineId.text
            }
        }

        if (index == -1) return null

        val line = lines[index]
        val safeChar = pos.char.coerceIn(0, line.text.length)

        return index to safeChar
    }

    private fun getSelectedText(lines: List<VisibleLine>): String {
        val aRaw = start ?: return ""
        val bRaw = end ?: return ""

        val a = resolvePos(aRaw, lines) ?: return selectedSnapshotText
        val b = resolvePos(bRaw, lines) ?: return selectedSnapshotText

        val from: Pair<Int, Int>
        val to: Pair<Int, Int>

        if (a.first < b.first || a.first == b.first && a.second <= b.second) {
            from = a
            to = b
        } else {
            from = b
            to = a
        }

        val result = StringBuilder()

        for (lineIndex in from.first..to.first) {
            val line = lines.getOrNull(lineIndex)?.text ?: continue

            val startChar = if (lineIndex == from.first) from.second else 0
            val endChar = if (lineIndex == to.first) to.second else line.length

            val safeStart = startChar.coerceIn(0, line.length)
            val safeEnd = endChar.coerceIn(0, line.length)

            if (safeEnd > safeStart) {
                result.append(line.substring(safeStart, safeEnd))
            }

            if (lineIndex != to.first) {
                result.append('\n')
            }
        }

        return result.toString()
    }

    fun renderSelection(
        context: DrawContext,
        textRenderer: TextRenderer,
        lines: List<VisibleLine>,
        chatLeft: Int,
        chatBottom: Int,
        lineHeight: Int,
        chatScale: Double,
        scrollOffset: Int
    ) {
        if (!hasSelection()) return

        val aRaw = start ?: return
        val bRaw = end ?: return

        val a = resolvePos(aRaw, lines) ?: return
        val b = resolvePos(bRaw, lines) ?: return

        val from: Pair<Int, Int>
        val to: Pair<Int, Int>

        if (a.first < b.first || a.first == b.first && a.second <= b.second) {
            from = a
            to = b
        } else {
            from = b
            to = a
        }

        for (lineIndex in from.first..to.first) {
            val visualLineIndex = lineIndex - scrollOffset
            if (visualLineIndex < 0) continue

            val line = lines.getOrNull(lineIndex)?.text ?: continue

            val startChar = if (lineIndex == from.first) from.second else 0
            val endChar = if (lineIndex == to.first) to.second else line.length

            val safeStart = startChar.coerceIn(0, line.length)
            val safeEnd = endChar.coerceIn(0, line.length)

            if (safeEnd <= safeStart) continue

            val before = line.substring(0, safeStart)
            val selected = line.substring(safeStart, safeEnd)

            val x1 = chatLeft + (textRenderer.getWidth(before) * chatScale).roundToInt()
            val x2 = chatLeft + (textRenderer.getWidth(before + selected) * chatScale).roundToInt()

            val scaledFontHeight = (textRenderer.fontHeight * chatScale)
                .roundToInt()
                .coerceAtLeast(1)

            val y2 = chatBottom - (visualLineIndex * lineHeight * chatScale).roundToInt()
            val y1 = y2 - scaledFontHeight

            context.fill(
                min(x1, x2),
                y1,
                max(x1, x2),
                y2,
                0xCC4A90FF.toInt()
            )
        }
    }

    fun mouseToLine(
        localMouseYFromBottom: Double,
        lineHeight: Int,
        visibleLineCount: Int
    ): Int? {
        val line = (localMouseYFromBottom / lineHeight).toInt()
        return if (line in 0 until visibleLineCount) line else null
    }

    fun mouseToChar(
        textRenderer: TextRenderer,
        line: String,
        localMouseX: Double
    ): Int {
        if (localMouseX <= 0.0) return 0

        for (i in 1..line.length) {
            val width = textRenderer.getWidth(line.substring(0, i))
            if (width >= localMouseX) return i
        }

        return line.length
    }

    fun visibleLinesToPlainText(lines: List<ChatHudLine.Visible>): List<VisibleLine> {
        return lines.map {
            val text = orderedTextToString(it.content())

            VisibleLine(
                id = LineId(
                    identityHash = System.identityHashCode(it),
                    text = text
                ),
                text = text
            )
        }
    }

    private fun orderedTextToString(orderedText: OrderedText): String {
        val builder = StringBuilder()

        orderedText.accept { _, _, codePoint ->
            builder.appendCodePoint(codePoint)
            true
        }

        return builder.toString()
    }
}