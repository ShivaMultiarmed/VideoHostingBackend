package mikhail.shell.video.hosting.domain

import mikhail.shell.video.hosting.domain.Result.Failure
import mikhail.shell.video.hosting.domain.Result.Success
import mikhail.shell.video.hosting.errors.Error

sealed class Result <out D, out E: Error> {
    data class Success<out D>(val data: D): Result<D, Nothing>()
    data class Failure<out E: Error>(val error: E): Result<Nothing, E>()
}

fun <D, E: Error> Result<D, E>.onSuccess(action: (D) -> Unit): Result<D, E> {
    if (this is Success) {
        action(data)
    }
    return this
}
fun <D, E: Error> Result<D, E>.onFailure(action: (E) -> Unit): Result<D, E> {
    if (this is Failure) {
        action(error)
    }
    return this
}

fun <E: Error> Result<*, E>.errorOrNull(): E? {
    return if (this is Failure) error else null
}

fun <D> Result<D, *>.dataOrNull(): D? {
    return if (this is Success) data else null
}