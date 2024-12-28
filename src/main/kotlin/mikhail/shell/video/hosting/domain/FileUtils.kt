package mikhail.shell.video.hosting.domain

import java.io.File

fun findFileByName(directory: File, fileName: String): File? {
    return directory.listFiles { dir, name ->
        name.parseFileName() == fileName
    }?.first()
}
fun String.parseExtension(): String {
    return this.substringAfterLast(".", "")
}
fun String.parseFileName(): String {
    return this.substringBeforeLast(".")
}