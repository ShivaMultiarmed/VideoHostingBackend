package mikhail.shell.video.hosting.domain.validation

import mikhail.shell.video.hosting.domain.FileValidator
import mikhail.shell.video.hosting.domain.ImagingValidator
import mikhail.shell.video.hosting.domain.Result
import mikhail.shell.video.hosting.errors.FileError
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.File

class ImageValidatorTests {
    private companion object {
        @JvmStatic
        lateinit var validator: FileValidator.ImageValidator

        @BeforeAll
        @JvmStatic
        fun initialize() {
            validator = ImagingValidator()
        }
    }

    @Test
    fun validate_ValidImage_ReturnsUnit() {
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
    fun validate_NotValidImage_ReturnsNotValidError() {
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