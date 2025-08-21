package mikhail.shell.video.hosting.errors

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

interface Error

data object UnexpectedError: Error

class UnauthenticatedException: RuntimeException()

class UniquenessViolationException: RuntimeException()

class ValidationException(val error: Error): RuntimeException()

class CompoundError<T: Error>(): Error {
    @JsonProperty("errors") private val _errors: MutableList<T> = mutableListOf()
    constructor(errors: List<T>): this() {
        _errors.addAll(errors)
    }
    constructor(vararg errors: T): this() {
        _errors.addAll(errors)
    }
    @get:JsonGetter(value = "errors")
    val errors get() = _errors.toList()

    fun add(error: T) {
        _errors.add(error)
    }
    operator fun plus(otherCompoundError: CompoundError<T>): CompoundError<T> {
        return CompoundError(errors + otherCompoundError.errors)
    }
    @JsonIgnore
    fun isEmpty(): Boolean {
        return _errors.isEmpty()
    }
    @JsonIgnore
    fun isNotEmpty(): Boolean {
        return _errors.isNotEmpty()
    }
    fun contains(error: Error): Boolean {
        return _errors.contains(error)
    }
}

fun <T: Error> Error?.equivalentTo(error: T): Boolean {
    return if (this is CompoundError<*>)
        this.contains(error)
    else
        this == error
}
fun Error?.isEmpty(): Boolean {
    return if (this is CompoundError<*>)
        this.isEmpty()
    else
        this == null
}

fun Error.toCompound(): CompoundError<Error> {
    return CompoundError<Error>().also { it.add(this) }
}

fun Error?.isNotEmpty(): Boolean = !isEmpty()