package mikhail.shell.video.hosting.domain

import java.io.InputStream

data class File(
    val name: String? = null,
    val mimeType: String? = null,
    val content: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (name != other.name) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode() = 31 * name.hashCode() + content.contentHashCode()
}

data class UploadedFile(
    val name: String,
    val mimeType: String,
    val inputStream: InputStream
)