package mikhail.shell.video.hosting.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "invalid_tokens")
class InvalidTokenEntity(@Id val token: String)