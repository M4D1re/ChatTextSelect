package ru.mrdire.chatselect

import net.fabricmc.api.ClientModInitializer

class ChatTextSelectClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Пока инициализация не нужна.
        // Вся работа идёт через mixin ChatScreenMixin.
    }
}