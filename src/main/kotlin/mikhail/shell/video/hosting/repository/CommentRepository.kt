package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.CommentEntity
import mikhail.shell.video.hosting.repository.entities.CommentWithUserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository: JpaRepository<CommentEntity, Long>

interface CommentWithUserRepository: JpaRepository<CommentWithUserEntity, Long>
