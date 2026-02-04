package mikhail.shell.video.hosting.domain

sealed class EditingAction<out I> {
    data object Keep : EditingAction<Nothing>()
    data object Remove : EditingAction<Nothing>()
    data class Edit<out I>(val value: I) : EditingAction<I>()
}