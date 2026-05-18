package ru.mrdire.chatselect.mixin

import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.gui.hud.ChatHudLine
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ChatHud::class)
interface ChatHudAccessor {
    @Accessor("visibleMessages")
    fun chatTextSelect_getVisibleMessages(): MutableList<ChatHudLine.Visible>

    @Accessor("scrolledLines")
    fun chatTextSelect_getScrolledLines(): Int
}