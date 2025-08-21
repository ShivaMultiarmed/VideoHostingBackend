package mikhail.shell.video.hosting.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class InvalidTokenEntity(@Id val token: String)