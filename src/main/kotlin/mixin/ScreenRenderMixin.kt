package ru.mrdire.chatselect.mixin

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import ru.mrdire.chatselect.ChatTextSelectState
import ru.mrdire.chatselect.ChatTextSelection

@Mixin(Screen::class)
abstract class ScreenRenderMixin {

    @Inject(method = ["extractRenderState"], at = [At("TAIL")])
    private fun onExtractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        tickDelta: Float,
        ci: CallbackInfo
    ) {
        val client = Minecraft.getInstance()

        if (!ChatTextSelectState.enabled) return
        if (client.screen !is ChatScreen) return
        if (!ChatTextSelection.hasSelection()) return

        val chatAccessor = client.gui.chat as ChatHudAccessor

        val lines = ChatTextSelection.visibleLinesToPlainText(
            chatAccessor.chatTextSelect_getTrimmedMessages()
        )

        val chatScale = chatAccessor.chatTextSelect_getScale()
        val lineHeight = chatAccessor.chatTextSelect_getLineHeight()
        val scrollOffset = chatAccessor.chatTextSelect_getChatScrollbarPos()

        val chatLeft = 1
        val chatBottom = client.window.guiScaledHeight - 40

        ChatTextSelection.renderSelection(
            graphics,
            client.font,
            lines,
            chatLeft,
            chatBottom,
            lineHeight,
            chatScale,
            scrollOffset
        )
    }
}