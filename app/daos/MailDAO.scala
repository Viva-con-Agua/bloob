package daos

import scala.concurrent.Future
import models.{Mail, MailStub}

trait MailDAO {
  def create(mail: Mail) : Future[Long]
  def create(mail: MailStub) : Future[Long]
  def all : Future[List[Mail]]
  def lookup(id: String) : Future[Option[Mail]]
  def lookup(id: Long) : Future[Option[Mail]]
  def send(id: Long) : Future[Option[Mail]]
}