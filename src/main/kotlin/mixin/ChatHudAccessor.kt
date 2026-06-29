package ru.mrdire.chatselect.mixin

import net.minecraft.client.gui.components.ChatComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(ChatComponent::class)
interface ChatHudAccessor {
    @Accessor("trimmedMessages")
    fun chatTextSelect_getTrimmedMessages(): MutableList<Any>

    @Accessor("chatScrollbarPos")
    fun chatTextSelect_getChatScrollbarPos(): Int

    @Invoker("getLineHeight")
    fun chatTextSelect_getLineHeight(): Int

    @Invoker("getScale")
    fun chatTextSelect_getScale(): Double
}