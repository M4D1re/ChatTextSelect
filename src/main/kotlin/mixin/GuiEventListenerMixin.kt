//package ru.mrdire.chatselect.mixin
//
//import net.minecraft.client.Minecraft
//import net.minecraft.client.gui.components.events.GuiEventListener
//import net.minecraft.client.gui.screens.ChatScreen
//import net.minecraft.client.input.MouseButtonEvent
//import org.lwjgl.glfw.GLFW
//import org.spongepowered.asm.mixin.Mixin
//import org.spongepowered.asm.mixin.Unique
//import org.spongepowered.asm.mixin.injection.At
//import org.spongepowered.asm.mixin.injection.Inject
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
//import ru.mrdire.chatselect.ChatTextSelectState
//import ru.mrdire.chatselect.ChatTextSelection
//
//@Mixin(GuiEventListener::class)
//interface GuiEventListenerMixin {
//
//    @Inject(method = ["mouseDragged"], at = [At("HEAD")], cancellable = false)
//    private fun onMouseDragged(
//        event: MouseButtonEvent,
//        dragX: Double,
//        dragY: Double,
//        cir: CallbackInfoReturnable<Boolean>
//    ) {
//        val client = Minecraft.getInstance()
//
//        if (!ChatTextSelectState.enabled) return
//        if (client.screen !is ChatScreen) return
//
//        val mouseX = chatTextSelect_mouseEventDouble(
//            event,
//            "x",
//            "mouseX",
//            "getX",
//            "getMouseX"
//        ) ?: return
//
//        val mouseY = chatTextSelect_mouseEventDouble(
//            event,
//            "y",
//            "mouseY",
//            "getY",
//            "getMouseY"
//        ) ?: return
//
//        val button = chatTextSelect_mouseEventInt(
//            event,
//            "button",
//            "key",
//            "getButton",
//            "buttonId"
//        ) ?: GLFW.GLFW_MOUSE_BUTTON_LEFT
//
//        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return
//
//        val chatAccessor = client.gui.chat as ChatHudAccessor
//
//        val lines = ChatTextSelection.visibleLinesToPlainText(
//            chatAccessor.chatTextSelect_getTrimmedMessages()
//        )
//
//        val scale = chatAccessor.chatTextSelect_getScale()
//        val lineHeight = chatAccessor.chatTextSelect_getLineHeight()
//        val scrollOffset = chatAccessor.chatTextSelect_getChatScrollbarPos()
//
//        val chatLeft = 1
//        val chatBottom = client.window.guiScaledHeight - 40
//
//        val localX = (mouseX - chatLeft) / scale
//        val localYFromBottom = (chatBottom - mouseY) / scale
//
//        val visualLineIndex = ChatTextSelection.mouseToLine(
//            localYFromBottom,
//            lineHeight,
//            lines.size
//        ) ?: return
//
//        val lineIndex = visualLineIndex + scrollOffset
//        if (lineIndex !in lines.indices) return
//
//        val line = lines[lineIndex].text
//
//        val charIndex = ChatTextSelection.mouseToChar(
//            client.font,
//            line,
//            localX
//        )
//
//        ChatTextSelection.dragIfMoved(
//            lines,
//            lineIndex,
//            charIndex
//        )
//
//        println("ChatTextSelect mouseDragged: lineIndex=$lineIndex, charIndex=$charIndex")
//    }
//
//    @Inject(method = ["mouseReleased"], at = [At("HEAD")], cancellable = false)
//    private fun onMouseReleased(
//        event: MouseButtonEvent,
//        cir: CallbackInfoReturnable<Boolean>
//    ) {
//        val client = Minecraft.getInstance()
//
//        if (!ChatTextSelectState.enabled) return
//        if (client.screen !is ChatScreen) return
//
//        val button = chatTextSelect_mouseEventInt(
//            event,
//            "button",
//            "key",
//            "getButton",
//            "buttonId"
//        ) ?: GLFW.GLFW_MOUSE_BUTTON_LEFT
//
//        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return
//
//        ChatTextSelection.finish()
//
//        println("ChatTextSelect mouseReleased")
//    }
//
//    @Unique
//    private fun chatTextSelect_mouseEventDouble(
//        event: Any,
//        vararg names: String
//    ): Double? {
//        for (name in names) {
//            val method = event.javaClass.methods.firstOrNull {
//                it.name == name && it.parameterCount == 0
//            }
//
//            val value = method?.invoke(event)
//
//            if (value is Number) {
//                return value.toDouble()
//            }
//        }
//
//        for (field in event.javaClass.declaredFields) {
//            if (field.name in names) {
//                field.isAccessible = true
//                val value = field.get(event)
//
//                if (value is Number) {
//                    return value.toDouble()
//                }
//            }
//        }
//
//        return null
//    }
//
//    @Unique
//    private fun chatTextSelect_mouseEventInt(
//        event: Any,
//        vararg names: String
//    ): Int? {
//        for (name in names) {
//            val method = event.javaClass.methods.firstOrNull {
//                it.name == name && it.parameterCount == 0
//            }
//
//            val value = method?.invoke(event)
//
//            if (value is Number) {
//                return value.toInt()
//            }
//        }
//
//        for (field in event.javaClass.declaredFields) {
//            if (field.name in names) {
//                field.isAccessible = true
//                val value = field.get(event)
//
//                if (value is Number) {
//                    return value.toInt()
//                }
//            }
//        }
//
//        return null
//    }
//}