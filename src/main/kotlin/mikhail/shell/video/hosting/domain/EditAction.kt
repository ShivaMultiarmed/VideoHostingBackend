package mikhail.shell.video.hosting.domain

enum class EditAction {
    KEEP, REMOVE, UPDATE
}

sealed class EditingAction<out I> {
    data object Keep : EditingAction<Nothing>()
    data object Remove : EditingAction<Nothing>()
    data class Edit<I>(val value: I) : EditingAction<I>()
}