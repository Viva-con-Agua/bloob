package mariadb

import javax.inject.{Inject, Singleton}

import daos.MailDAO

import scala.concurrent.{ExecutionContext, Future}
import models.Mail

@Singleton
class MailDAOImpl @Inject()(mailDBDAO: DBMailDAOImpl,receiverDAO: ReceiverDAOImpl)(implicit ec: ExecutionContext) extends MailDAO {

  def create(mail: Mail) : Future[Long] = mailDBDAO.create(DBMail(mail))

  def all : Future[List[Mail]] = mailDBDAO.all.flatMap((l) => Future.sequence(
    l.map((m) => {
      val dbReceiver = receiverDAO.findByMailId(m.id)
      dbReceiver.map(list =>  Mail(m.authorEmail, m.subject, m.body, list.map(_.userEmail).toSet))
    })
  ))
}