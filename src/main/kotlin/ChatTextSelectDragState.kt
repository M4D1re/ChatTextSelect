package ru.mrdire.chatselect

object ChatTextSelectDragState {
    private const val DRAG_THRESHOLD = 5.0

    var mouseDown: Boolean = false
        private set

    private var startMouseX: Double = 0.0
    private var startMouseY: Double = 0.0

    fun start(mouseX: Double, mouseY: Double) {
        mouseDown = true
        startMouseX = mouseX
        startMouseY = mouseY
    }

    fun stop() {
        mouseDown = false
    }

    fun hasMovedEnough(mouseX: Double, mouseY: Double): Boolean {
        if (!mouseDown) return false

        val movedX = mouseX - startMouseX
        val movedY = mouseY - startMouseY
        val movedDistanceSq = movedX * movedX + movedY * movedY

        return movedDistanceSq >= DRAG_THRESHOLD * DRAG_THRESHOLD
    }
}