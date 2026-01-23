package mikhail.shell.video.hosting.domain.validation

import mikhail.shell.video.hosting.domain.FileValidator
import mikhail.shell.video.hosting.domain.Jave2VideoValidator
import mikhail.shell.video.hosting.domain.Result
import mikhail.shell.video.hosting.errors.FileError
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.File

class VideoValidatorTests {
    private companion object {
        @JvmStatic
        lateinit var validator: FileValidator.VideoValidator
        @BeforeAll
        @JvmStatic
        fun initialize () {
            validator = Jave2VideoValidator()
        }
    }
    @Test
    fun validate_ValidVideo_ReturnsUnit() {
        val files = listOf<File>() // TODO
        val expected = Result.Success(Unit)
        Assertions.assertAll(
            files.map { file ->
                Executable {
                    Assertions.assertEquals(expected, validator.validate(file))
                }
            }
        )
    }
    @Test
    fun validate_InvalidVideo_ReturnsNotValidError() {
        val files = listOf<File>() // TODO
        val expected = Result.Failure(FileError.NOT_VALID)
        Assertions.assertAll(
            files.map { file ->
                Executable {
                    Assertions.assertEquals(expected, validator.validate(file))
                }
            }
        )
    }
}