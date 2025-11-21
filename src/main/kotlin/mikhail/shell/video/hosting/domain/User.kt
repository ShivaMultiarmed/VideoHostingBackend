package mikhail.shell.video.hosting.domain

data class User(
    val userId: Long,
    val nick: String,
    val name: String?,
    val bio: String?,
    val tel: String?,
    val email: String?
)

data class UserCreatingModel(
    val nick: String
)

data class UserEditingModel(
    val userId: Long,
    val nick: String,
    val name: String?,
    val bio: String?,
    val tel: String?,
    val email: String?,
    val avatar: UploadedFile?,
    val avatarAction: EditAction
)