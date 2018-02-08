package daos

import scala.concurrent.Future
import models.Mail

trait MailDAO {
  def create(mail: Mail) : Future[Long]
  def all : Future[List[Mail]]
}