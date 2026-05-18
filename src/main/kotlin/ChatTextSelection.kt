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
        val line: Int,
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

    fun begin(line: Int, char: Int) {
        selecting = true
        start = Pos(line, char)
        end = Pos(line, char)
    }

    fun drag(line: Int, char: Int) {
        if (selecting) {
            end = Pos(line, char)
        }
    }

    fun finish() {
        selecting = false
    }

    fun hasSelection(): Boolean {
        val a = start
        val b = end
        return a != null && b != null && a != b
    }

    fun copyToClipboard(client: MinecraftClient, lines: List<String>) {
        val text = getSelectedText(lines)
        if (text.isNotBlank()) {
            client.keyboard.clipboard = text
        }
    }

    fun getSelectedText(lines: List<String>): String {
        val a = start ?: return ""
        val b = end ?: return ""

        val from: Pos
        val to: Pos

        if (a.line < b.line || a.line == b.line && a.char <= b.char) {
            from = a
            to = b
        } else {
            from = b
            to = a
        }

        val result = StringBuilder()

        for (lineIndex in from.line..to.line) {
            val line = lines.getOrNull(lineIndex) ?: continue

            val startChar = if (lineIndex == from.line) from.char else 0
            val endChar = if (lineIndex == to.line) to.char else line.length

            val safeStart = startChar.coerceIn(0, line.length)
            val safeEnd = endChar.coerceIn(0, line.length)

            if (safeEnd > safeStart) {
                result.append(line.substring(safeStart, safeEnd))
            }

            if (lineIndex != to.line) {
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
        lineHeight: Int
    ) {
        val a = start ?: return
        val b = end ?: return

        val from: Pos
        val to: Pos

        if (a.line < b.line || a.line == b.line && a.char <= b.char) {
            from = a
            to = b
        } else {
            from = b
            to = a
        }

        for (lineIndex in from.line..to.line) {
            val line = lines.getOrNull(lineIndex) ?: continue

            val startChar = if (lineIndex == from.line) from.char else 0
            val endChar = if (lineIndex == to.line) to.char else line.length

            val safeStart = startChar.coerceIn(0, line.length)
            val safeEnd = endChar.coerceIn(0, line.length)

            if (safeEnd <= safeStart) continue

            val before = line.substring(0, safeStart)
            val selected = line.substring(safeStart, safeEnd)

            val x1 = chatLeft + textRenderer.getWidth(before)
            val x2 = x1 + textRenderer.getWidth(selected)

            val y = chatBottom - (lineIndex + 1) * lineHeight

            context.fill(
                min(x1, x2),
                y,
                max(x1, x2),
                y + lineHeight,
                0x663399FF
            )
        }
    }

    fun mouseToLine(
        mouseY: Double,
        chatBottom: Int,
        lineHeight: Int,
        visibleLineCount: Int
    ): Int? {
        val line = ((chatBottom - mouseY) / lineHeight).toInt()
        return if (line in 0 until visibleLineCount) line else null
    }

    fun mouseToChar(
        textRenderer: TextRenderer,
        line: String,
        mouseX: Double,
        chatLeft: Int
    ): Int {
        val localX = (mouseX - chatLeft).toInt()

        if (localX <= 0) return 0

        for (i in 1..line.length) {
            val width = textRenderer.getWidth(line.substring(0, i))
            if (width >= localX) return i
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