package mikhail.shell.video.hosting.domain

data class User(
    val userId: Long? = null,
    val name: String? = null,
    val nick: String,
    val bio: String? = null,
    val tel: Int? = null,
    val email: String? = null
)
