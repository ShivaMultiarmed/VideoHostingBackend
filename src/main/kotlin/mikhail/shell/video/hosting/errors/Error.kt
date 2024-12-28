package mikhail.shell.video.hosting.errors

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

interface Error

data class CompoundError<T: Error>(
    @JsonProperty("errors") private val _errors: MutableList<T> = mutableListOf()
): Error {
    @get:JsonGetter(value = "errors")
    val errors get() = _errors.toList()

    fun add(error: T) {
        _errors.add(error)
    }
    operator fun plus(compoundError: CompoundError<T>): CompoundError<T> {
        _errors.addAll(compoundError.errors)
        return this
    }
    @JsonIgnore
    fun isNull(): Boolean {
        return _errors.isEmpty()
    }
    @JsonIgnore
    fun isNotNull(): Boolean {
        return _errors.isNotEmpty()
    }
    fun contains(error: T): Boolean {
        return _errors.contains(error)
    }
}