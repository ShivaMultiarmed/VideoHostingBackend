package mikhail.shell.video.hosting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository: JpaRepository<UserEntity, Long> {
    fun existsByNick(nick: String): Boolean
}