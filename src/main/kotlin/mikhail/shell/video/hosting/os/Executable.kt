package mikhail.shell.video.hosting.os

class Executable(private val exe: String) {
    private val runtime = Runtime.getRuntime()
    fun execute(command: Array<String>): Int {
        return try {
            val process = runtime.exec(arrayOf(exe) + command)
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}