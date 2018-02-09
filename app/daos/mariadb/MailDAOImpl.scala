package mariadb

import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import daos.MailDAO

import scala.concurrent.{ExecutionContext, Future}
import models.{Mail, MailMeta}

@Singleton
class MailDAOImpl @Inject()(mailDBDAO: DBMailDAOImpl,receiverDAO: ReceiverDAOImpl)(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends MailDAO {

  // We want the JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private def mailReceiverInnerJoin = for {
    (m, r) <- mailDBDAO.mails join receiverDAO.receiver on(_.id === _.mail_id)
  } yield (m, r)

  def create(mail: Mail) : Future[Long] = mailDBDAO.create(DBMail(mail), mail.receiver)

  def all : Future[List[Mail]] = db.run(mailReceiverInnerJoin.result).map( result =>
    result.foldLeft[Map[Long, Mail]](Map())((list, mailReceiver) => {
      val mail = list.get(mailReceiver._1.id).map((mail) =>
        mail.copy(receiver = mail.receiver ++ Set(mailReceiver._2.userEmail))
      ).getOrElse({
        val meta = MailMeta(mailReceiver._1.metaSendingAddress, mailReceiver._1.metaCreated, mailReceiver._1.metaSent)
        Mail(mailReceiver._1.authorEmail, mailReceiver._1.subject, mailReceiver._1.body, Set(mailReceiver._2.userEmail), meta)
      })
      (list - mailReceiver._1.id) + (mailReceiver._1.id -> mail)
    }).toList.map(_._2)
  )
}