package mikhail.shell.video.hosting.controllers

fun constructReferenceBaseApiUrl(
    protocol: String,
    referenceHost: String
) = "$protocol://$referenceHost/api"