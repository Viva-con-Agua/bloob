package daos

import java.util.UUID
import scala.concurrent.Future
import models.User

trait UserDAO {
  def findByUUID(uuid: UUID) : Future[Option[User]]
}