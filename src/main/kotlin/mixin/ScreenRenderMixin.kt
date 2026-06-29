package ru.mrdire.chatselect.mixin

import net.minecraft.client.Minecraft
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

    @Unique
    private fun chatTextSelect_client(): Minecraft {
        return Minecraft.getInstance()
    }

    @Unique
    private fun chatTextSelect_accessor(): ChatHudAccessor {
        return chatTextSelect_client().gui.chat as ChatHudAccessor
    }

    @Unique
    private fun chatTextSelect_plainLines(): List<ChatTextSelection.VisibleLine> {
        return ChatTextSelection.visibleLinesToPlainText(
            chatTextSelect_accessor().chatTextSelect_getTrimmedMessages()
        )
    }

    @Unique
    private fun chatTextSelect_chatLeft(): Int {
        return 1
    }

    @Unique
    private fun chatTextSelect_chatBottom(): Int {
        val client = chatTextSelect_client()
        return client.window.guiScaledHeight - 40
    }

    @Unique
    private fun chatTextSelect_chatScale(): Double {
        return chatTextSelect_accessor().chatTextSelect_getScale()
    }

    @Unique
    private fun chatTextSelect_lineHeight(): Int {
        return chatTextSelect_accessor().chatTextSelect_getLineHeight()
    }

    @Unique
    private fun chatTextSelect_scrollOffset(): Int {
        return chatTextSelect_accessor().chatTextSelect_getChatScrollbarPos()
    }

    @Inject(method = ["render"], at = [At("TAIL")])
    private fun onRender(ci: CallbackInfo) {
        if (!ChatTextSelectState.enabled) return

        val client = chatTextSelect_client()

        if (client.screen !is ChatScreen) return
        if (!ChatTextSelection.hasSelection()) return

        val screen = client.screen ?: return

        val mouseXField = screen.javaClass.superclass
            .declaredFields
            .firstOrNull { it.name == "mouseX" }

        val mouseYField = screen.javaClass.superclass
            .declaredFields
            .firstOrNull { it.name == "mouseY" }

        val lines = chatTextSelect_plainLines()
        val scale = chatTextSelect_chatScale()
        val scrollOffset = chatTextSelect_scrollOffset()


    }
}