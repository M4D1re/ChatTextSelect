package ru.mrdire.chatselect.mixin

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import ru.mrdire.chatselect.ChatTextSelectState
import ru.mrdire.chatselect.ChatTextSelection

@Mixin(ChatScreen::class)
abstract class ChatScreenMixin {

    @Unique
    private var chatTextSelect_mouseDown = false

    @Unique
    private var chatTextSelect_startMouseX = 0.0

    @Unique
    private var chatTextSelect_startMouseY = 0.0

    @Unique
    private val chatTextSelect_dragThreshold = 5.0

    @Unique
    private fun chatTextSelect_client(): MinecraftClient {
        return MinecraftClient.getInstance()
    }

    @Unique
    private fun chatTextSelect_plainLines(): List<ChatTextSelection.VisibleLine> {
        val client = chatTextSelect_client()
        val accessor = client.inGameHud.chatHud as ChatHudAccessor

        return ChatTextSelection.visibleLinesToPlainText(
            accessor.chatTextSelect_getVisibleMessages()
        )
    }

    @Unique
    private fun chatTextSelect_chatLeft(): Int {
        return 1
    }

    @Unique
    private fun chatTextSelect_chatBottom(): Int {
        val client = chatTextSelect_client()
        return client.window.scaledHeight - 40
    }

    @Unique
    private fun chatTextSelect_chatScale(): Double {
        val client = chatTextSelect_client()
        return client.options.chatScale.value
    }

    @Unique
    private fun chatTextSelect_lineHeight(): Int {
        val client = chatTextSelect_client()
        val accessor = client.inGameHud.chatHud as ChatHudAccessor
        return accessor.chatTextSelect_getLineHeight()
    }

    @Inject(method = ["mouseClicked"], at = [At("HEAD")], cancellable = false)
    private fun onMouseClicked(
        click: Click,
        doubled: Boolean,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (!ChatTextSelectState.enabled) return
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return

        val client = chatTextSelect_client()
        val lines = chatTextSelect_plainLines()
        val scale = chatTextSelect_chatScale()

        val scrollOffset = chatTextSelect_scrollOffset()

        val localX = (click.x() - chatTextSelect_chatLeft()) / scale
        val localYFromBottom = (chatTextSelect_chatBottom() - click.y()) / scale

        val visualLineIndex = ChatTextSelection.mouseToLine(
            localYFromBottom,
            chatTextSelect_lineHeight(),
            lines.size
        ) ?: return

        val lineIndex = visualLineIndex + scrollOffset
        if (lineIndex !in lines.indices) return

        val line = lines[lineIndex].text

        val charIndex = ChatTextSelection.mouseToChar(
            client.textRenderer,
            line,
            localX
        )

        if (doubled) {
            ChatTextSelection.selectWord(
                lines,
                lineIndex,
                charIndex
            )
        } else {
            chatTextSelect_mouseDown = true
            chatTextSelect_startMouseX = click.x()
            chatTextSelect_startMouseY = click.y()

            ChatTextSelection.prepare(
                lines,
                lineIndex,
                charIndex
            )
        }
    }

    @Inject(method = ["keyPressed"], at = [At("HEAD")], cancellable = true)
    private fun onKeyPressed(
        input: KeyInput,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (!ChatTextSelectState.enabled) return

        if (input.isCopy && ChatTextSelection.hasSelection()) {
            ChatTextSelection.copyToClipboard(chatTextSelect_client())
            cir.returnValue = true
        }
    }

    @Unique
    private fun chatTextSelect_scrollOffset(): Int {
        val client = chatTextSelect_client()
        val accessor = client.inGameHud.chatHud as ChatHudAccessor
        return accessor.chatTextSelect_getScrolledLines()
    }

    @Inject(method = ["render"], at = [At("TAIL")])
    private fun onRender(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
        ci: CallbackInfo
    ) {
        if (!ChatTextSelectState.enabled) return

        val client = chatTextSelect_client()
        val lines = chatTextSelect_plainLines()
        val scale = chatTextSelect_chatScale()

        val scrollOffset = chatTextSelect_scrollOffset()

        val leftPressed = GLFW.glfwGetMouseButton(
            client.window.handle,
            GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS

        if (!leftPressed) {
            chatTextSelect_mouseDown = false
        }

        if (leftPressed) {
            val localX = (mouseX - chatTextSelect_chatLeft()) / scale
            val localYFromBottom = (chatTextSelect_chatBottom() - mouseY) / scale

            val visualLineIndex = ChatTextSelection.mouseToLine(
                localYFromBottom,
                chatTextSelect_lineHeight(),
                lines.size
            )

            val lineIndex = if (visualLineIndex != null) {
                visualLineIndex + scrollOffset
            } else {
                null
            }

            if (lineIndex != null) {
                val line = lines[lineIndex].text

                val charIndex = ChatTextSelection.mouseToChar(
                    client.textRenderer,
                    line,
                    localX
                )

                val movedX = mouseX - chatTextSelect_startMouseX
                val movedY = mouseY - chatTextSelect_startMouseY
                val movedDistanceSq = movedX * movedX + movedY * movedY

                if (
                    chatTextSelect_mouseDown &&
                    movedDistanceSq >= chatTextSelect_dragThreshold * chatTextSelect_dragThreshold
                ) {
                    ChatTextSelection.dragIfMoved(
                        lines,
                        lineIndex,
                        charIndex
                    )
                }
            }
        }

        ChatTextSelection.renderSelection(
            context,
            client.textRenderer,
            lines,
            chatTextSelect_chatLeft(),
            chatTextSelect_chatBottom(),
            chatTextSelect_lineHeight(),
            scale,
            scrollOffset
        )
    }
}