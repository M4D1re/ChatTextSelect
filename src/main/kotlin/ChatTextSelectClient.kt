package ru.mrdire.chatselect

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class ChatTextSelectClient : ClientModInitializer {
    override fun onInitializeClient() {
        val toggleKey = KeyBinding(
            "key.chattextselect.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_ALT,
            KeyBinding.Category.MISC
        )

        KeyBindingHelper.registerKeyBinding(toggleKey)

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleKey.wasPressed()) {
                val enabled = ChatTextSelectState.toggle()

                client.inGameHud.chatHud.addMessage(
                    Text.literal(
                        if (enabled) {
                            "Chat Text Select: enabled"
                        } else {
                            "Chat Text Select: disabled"
                        }
                    )
                )
            }
        }
    }
}