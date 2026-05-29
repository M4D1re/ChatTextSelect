package ru.mrdire.chatselect

object ChatTextSelectState {
    var enabled: Boolean = true

    fun toggle(): Boolean {
        enabled = !enabled

        if (!enabled) {
            ChatTextSelection.clear()
        }

        return enabled
    }
}