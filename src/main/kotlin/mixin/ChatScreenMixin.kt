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
import ru.mrdire.chatselect.ChatTextSelection

@Mixin(ChatScreen::class)
abstract class ChatScreenMixin {
    @Unique
    private fun chatTextSelect_client(): MinecraftClient {
        return MinecraftClient.getInstance()
    }

    @Unique
    private fun chatTextSelect_plainLines(): List<String> {
        val client = chatTextSelect_client()
        val accessor = client.inGameHud.chatHud as ChatHudAccessor

        return ChatTextSelection.visibleLinesToPlainText(
            accessor.chatTextSelect_getVisibleMessages()
        )
    }

    @Unique
    private fun chatTextSelect_chatLeft(): Int {
        return 4
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

        val fontHeight = client.textRenderer.fontHeight

        val spacing = client.options.chatLineSpacing.value

        return (fontHeight + spacing * 8.0).toInt()
    }

    @Inject(method = ["mouseClicked"], at = [At("HEAD")], cancellable = true)
    private fun onMouseClicked(
        click: Click,
        doubled: Boolean,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return

        val client = chatTextSelect_client()
        val lines = chatTextSelect_plainLines()

        val scale = chatTextSelect_chatScale()
        val localX = (click.x() - chatTextSelect_chatLeft()) / scale
        val localYFromBottom = (chatTextSelect_chatBottom() - click.y()) / scale

        val lineIndex = ChatTextSelection.mouseToLine(
            localYFromBottom,
            chatTextSelect_lineHeight(),
            lines.size
        ) ?: return

        val line = lines[lineIndex]
        val charIndex = ChatTextSelection.mouseToChar(
            client.textRenderer,
            line,
            localX
        )

        ChatTextSelection.begin(line, charIndex)
        cir.returnValue = true
    }

    @Inject(method = ["keyPressed"], at = [At("HEAD")], cancellable = true)
    private fun onKeyPressed(
        input: KeyInput,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        if (input.isCopy && ChatTextSelection.hasSelection()) {
            ChatTextSelection.copyToClipboard(
                chatTextSelect_client(),
                chatTextSelect_plainLines()
            )

            cir.returnValue = true
        }
    }

    @Inject(method = ["render"], at = [At("TAIL")])
    private fun onRender(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
        ci: CallbackInfo
    ) {
        val client = chatTextSelect_client()
        val lines = chatTextSelect_plainLines()
        val scale = chatTextSelect_chatScale()

        if (GLFW.glfwGetMouseButton(
                client.window.handle,
                GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == GLFW.GLFW_PRESS
        ) {
            val localX = (mouseX - chatTextSelect_chatLeft()) / scale
            val localYFromBottom = (chatTextSelect_chatBottom() - mouseY) / scale

            val lineIndex = ChatTextSelection.mouseToLine(
                localYFromBottom,
                chatTextSelect_lineHeight(),
                lines.size
            )

            if (lineIndex != null) {
                val line = lines[lineIndex]

                val charIndex = ChatTextSelection.mouseToChar(
                    client.textRenderer,
                    line,
                    localX
                )

                ChatTextSelection.drag(line, charIndex)  }
        }

        ChatTextSelection.renderSelection(
            context,
            client.textRenderer,
            lines,
            chatTextSelect_chatLeft(),
            chatTextSelect_chatBottom(),
            chatTextSelect_lineHeight(),
            scale
        )
    }
}