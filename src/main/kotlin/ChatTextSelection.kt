package ru.mrdire.chatselect

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.util.FormattedCharSequence
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

    fun copyToClipboard(client: Minecraft) {
        if (selectedSnapshotText.isNotBlank()) {
            client.keyboardHandler.clipboard = selectedSnapshotText
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

    fun selectLine(
        lines: List<VisibleLine>,
        lineIndex: Int
    ) {
        val visibleLine = lines.getOrNull(lineIndex) ?: return
        val line = visibleLine.text

        start = Pos(visibleLine.id, 0)
        end = Pos(visibleLine.id, line.length)

        pending = false
        selecting = false

        selectedSnapshotText = line
    }

    private fun normalizeVisualRange(
        a: Pair<Int, Int>,
        b: Pair<Int, Int>
    ): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val aBeforeB =
            a.first > b.first ||
                    a.first == b.first && a.second <= b.second

        return if (aBeforeB) {
            a to b
        } else {
            b to a
        }
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

        val normalized = normalizeVisualRange(a, b)

        val top = normalized.first
        val bottom = normalized.second

        val result = StringBuilder()

        val lineRange =
            if (top.first >= bottom.first) {
                top.first downTo bottom.first
            } else {
                top.first..bottom.first
            }

        for (lineIndex in lineRange) {
            val line = lines.getOrNull(lineIndex)?.text ?: continue

            val startChar: Int
            val endChar: Int

            if (top.first == bottom.first) {
                startChar = min(top.second, bottom.second)
                endChar = max(top.second, bottom.second)
            } else {
                when (lineIndex) {
                    top.first -> {
                        startChar = top.second
                        endChar = line.length
                    }

                    bottom.first -> {
                        startChar = 0
                        endChar = bottom.second
                    }

                    else -> {
                        startChar = 0
                        endChar = line.length
                    }
                }
            }

            val safeStart = startChar.coerceIn(0, line.length)
            val safeEnd = endChar.coerceIn(0, line.length)

            if (safeEnd > safeStart) {
                result.append(line.substring(safeStart, safeEnd))
            }

            if (lineIndex != bottom.first) {
                result.append('\n')
            }
        }

        return result.toString()
    }

    fun renderSelection(
        graphics: Any,
        font: Font,
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

        val normalized = normalizeVisualRange(a, b)

        val top = normalized.first
        val bottom = normalized.second

        val lineRange =
            if (top.first >= bottom.first) {
                top.first downTo bottom.first
            } else {
                top.first..bottom.first
            }

        for (lineIndex in lineRange) {
            val visualLineIndex = lineIndex - scrollOffset
            if (visualLineIndex < 0) continue

            val line = lines.getOrNull(lineIndex)?.text ?: continue

            val startChar: Int
            val endChar: Int

            if (top.first == bottom.first) {
                startChar = min(top.second, bottom.second)
                endChar = max(top.second, bottom.second)
            } else {
                when (lineIndex) {
                    top.first -> {
                        startChar = top.second
                        endChar = line.length
                    }

                    bottom.first -> {
                        startChar = 0
                        endChar = bottom.second
                    }

                    else -> {
                        startChar = 0
                        endChar = line.length
                    }
                }
            }

            val safeStart = startChar.coerceIn(0, line.length)
            val safeEnd = endChar.coerceIn(0, line.length)

            if (safeEnd <= safeStart) continue

            val before = line.substring(0, safeStart)
            val selected = line.substring(safeStart, safeEnd)

            val x1 = chatLeft + (font.width(before) * chatScale).roundToInt()
            val x2 = chatLeft + (font.width(before + selected) * chatScale).roundToInt()

            val lineBottom = chatBottom - (visualLineIndex * lineHeight * chatScale).roundToInt()
            val lineTop = chatBottom - ((visualLineIndex + 1) * lineHeight * chatScale).roundToInt()

            val y1 = lineTop
            val y2 = lineBottom

            fill(
                graphics,
                min(x1, x2),
                y1,
                max(x1, x2),
                y2,
                0xCC4A90FF.toInt()
            )
        }
    }

    private fun fill(
        graphics: Any,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        color: Int
    ) {
        val method = graphics.javaClass.methods.firstOrNull {
            it.name == "fill" &&
                    it.parameterTypes.size == 5 &&
                    it.parameterTypes.all { type -> type == Int::class.javaPrimitiveType }
        } ?: return

        method.invoke(graphics, x1, y1, x2, y2, color)
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
        font: Font,
        line: String,
        localMouseX: Double
    ): Int {
        if (localMouseX <= 0.0) return 0

        for (i in 1..line.length) {
            val width = font.width(line.substring(0, i))
            if (width >= localMouseX) return i
        }

        return line.length
    }

    fun visibleLinesToPlainText(lines: List<Any>): List<VisibleLine> {
        return lines.map {
            val text = visibleLineToString(it)

            VisibleLine(
                id = LineId(
                    identityHash = System.identityHashCode(it),
                    text = text
                ),
                text = text
            )
        }
    }

    private fun visibleLineToString(line: Any): String {
        val content = line.javaClass.methods
            .firstOrNull { it.name == "content" && it.parameterCount == 0 }
            ?.invoke(line)

        if (content is FormattedCharSequence) {
            return formattedTextToString(content)
        }

        return content?.toString() ?: line.toString()
    }

    private fun formattedTextToString(text: FormattedCharSequence): String {
        val builder = StringBuilder()

        text.accept { _, _, codePoint ->
            builder.appendCodePoint(codePoint)
            true
        }

        return builder.toString()
    }
}