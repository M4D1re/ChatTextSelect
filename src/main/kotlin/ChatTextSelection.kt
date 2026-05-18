package ru.mrdire.chatselect

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.text.OrderedText
import net.minecraft.text.StringVisitable
import kotlin.math.max
import kotlin.math.min

object ChatTextSelection {
    private data class Pos(
        val lineText: String,
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

    fun begin(lineText: String, char: Int) {
        selecting = true
        start = Pos(lineText, char)
        end = Pos(lineText, char)
    }

    fun drag(lineText: String, char: Int) {
        if (selecting) {
            end = Pos(lineText, char)
        }
    }

    fun finish() {
        selecting = false
    }

    fun hasSelection(): Boolean {
        val a = start ?: return false
        val b = end ?: return false
        return a != b
    }

    fun copyToClipboard(client: MinecraftClient, lines: List<String>) {
        val text = getSelectedText(lines)
        if (text.isNotBlank()) {
            client.keyboard.clipboard = text
        }
    }

    private fun resolvePos(pos: Pos, lines: List<String>): Pair<Int, Int>? {
        val lineIndex = lines.indexOf(pos.lineText)
        if (lineIndex == -1) return null

        val safeChar = pos.char.coerceIn(0, pos.lineText.length)
        return lineIndex to safeChar
    }

    fun getSelectedText(lines: List<String>): String {
        val aRaw = start ?: return ""
        val bRaw = end ?: return ""

        val aResolved = resolvePos(aRaw, lines) ?: return ""
        val bResolved = resolvePos(bRaw, lines) ?: return ""

        val a = aResolved
        val b = bResolved

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
            val line = lines.getOrNull(lineIndex) ?: continue

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
        lines: List<String>,
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
            val line = lines.getOrNull(lineIndex) ?: continue

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
    fun visibleLinesToPlainText(lines: List<ChatHudLine.Visible>): List<String> {
        return lines.map { orderedTextToString(it.content()) }
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