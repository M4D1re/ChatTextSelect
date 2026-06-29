package ru.mrdire.chatselect

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

class ChatTextSelectClient : ClientModInitializer {
    override fun onInitializeClient() {

        val toggleKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.chattextselect.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                KeyMapping.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register {
            while (toggleKey.consumeClick()) {
                ChatTextSelectState.toggle()
            }
        }
    }
}