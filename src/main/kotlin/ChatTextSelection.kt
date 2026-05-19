package ru.mrdire.chatselect

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.text.OrderedText
import kotlin.math.max
import kotlin.math.min

object ChatTextSelection {
    data class VisibleLine(
        val id: LineId,
        val text: String
    )

    data class LineId(
        val addedTime: Int,
        val endOfEntry: Boolean,
        val text: String
    )

    private data class Pos(
        val lineId: LineId,
        val char: Int
    )

    private var selecting = false
    private var start: Pos? = null
    private var end: Pos? = null

    fun clear() {
        selecting = false
        start = null
        end = null
    }

    fun begin(lines: List<VisibleLine>, lineIndex: Int, char: Int) {
        val line = lines.getOrNull(lineIndex) ?: return

        selecting = true
        start = Pos(line.id, char)
        end = Pos(line.id, char)
    }

    fun drag(lines: List<VisibleLine>, lineIndex: Int, char: Int) {
        if (!selecting) return

        val line = lines.getOrNull(lineIndex) ?: return
        end = Pos(line.id, char)
    }

    fun finish() {
        selecting = false
    }

    fun hasSelection(): Boolean {
        val a = start ?: return false
        val b = end ?: return false
        return a != b
    }

    fun copyToClipboard(client: MinecraftClient, lines: List<VisibleLine>) {
        val text = getSelectedText(lines)
        if (text.isNotBlank()) {
            client.keyboard.clipboard = text
        }
    }

    private fun resolvePos(pos: Pos, lines: List<VisibleLine>): Pair<Int, Int>? {
        val index = lines.indexOfFirst { it.id == pos.lineId }
        if (index == -1) return null

        val line = lines[index]
        val safeChar = pos.char.coerceIn(0, line.text.length)

        return index to safeChar
    }

    fun getSelectedText(lines: List<VisibleLine>): String {
        val aRaw = start ?: return ""
        val bRaw = end ?: return ""

        val a = resolvePos(aRaw, lines) ?: return ""
        val b = resolvePos(bRaw, lines) ?: return ""

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
        chatScale: Double
    ) {
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
            val line = lines.getOrNull(lineIndex)?.text ?: continue

            val startChar = if (lineIndex == from.first) from.second else 0
            val endChar = if (lineIndex == to.first) to.second else line.length

            val safeStart = startChar.coerceIn(0, line.length)
            val safeEnd = endChar.coerceIn(0, line.length)

            if (safeEnd <= safeStart) continue

            val before = line.substring(0, safeStart)
            val selected = line.substring(safeStart, safeEnd)

            val x1 = chatLeft + (textRenderer.getWidth(before) * chatScale).toInt()
            val x2 = chatLeft + (textRenderer.getWidth(before + selected) * chatScale).toInt()

            val scaledLineHeight = (lineHeight * chatScale).toInt().coerceAtLeast(1)
            val scaledFontHeight = (textRenderer.fontHeight * chatScale).toInt().coerceAtLeast(1)

            val y2 = chatBottom - (lineIndex * scaledLineHeight)
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

    fun selectWord(
        lines: List<VisibleLine>,
        lineIndex: Int,
        charIndex: Int
    ) {
        val line = lines.getOrNull(lineIndex)?.text ?: return

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

        val lineId = lines[lineIndex].id

        start = Pos(lineId, startIndex)
        end = Pos(lineId, endIndex + 1)

        selecting = false
    }

    fun visibleLinesToPlainText(lines: List<ChatHudLine.Visible>): List<VisibleLine> {
        return lines.map {
            val text = orderedTextToString(it.content())

            VisibleLine(
                id = LineId(
                    addedTime = it.addedTime(),
                    endOfEntry = it.endOfEntry(),
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