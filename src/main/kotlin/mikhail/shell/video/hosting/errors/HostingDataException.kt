package mikhail.shell.video.hosting.errors

class HostingDataException(
    val compoundError: CompoundError<out Error>
): RuntimeException()