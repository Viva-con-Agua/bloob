package mariadb

import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import daos.MailDAO

import scala.concurrent.{ExecutionContext, Future}
import models.{Mail, MailMeta, MailStub}

@Singleton
class MailDAOImpl @Inject()(mailDBDAO: DBMailDAOImpl,receiverDAO: ReceiverDAOImpl)(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends MailDAO {

  // We want the JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private def mailReceiver(idOpt: Option[Long]) = for {
    (m, r) <- mailDBDAO.mails.filter(m =>
      idOpt.map(id => m.id === id).getOrElse(slick.lifted.LiteralColumn(true))
    ) joinLeft receiverDAO.receiver on(_.id === _.mail_id)
  } yield (m, r)

  private def resultMapper(result: Seq[(DBMail, Option[DBReceiver])]) : List[Mail] =
    result.foldLeft[Map[Long, Mail]](Map())((list, mailReceiver) => {
      val mail = list.get(mailReceiver._1.id).map((mail) =>
        mail.copy(receiver = mail.receiver ++ mailReceiver._2.map(dbReceiver => Set(dbReceiver.userEmail)).getOrElse(Set()))
      ).getOrElse({
        val meta = MailMeta(mailReceiver._1.metaSendingAddress, mailReceiver._1.metaCreated, mailReceiver._1.metaSent)
        val receiver = mailReceiver._2.map(dbReceiver => Set(dbReceiver.userEmail)).getOrElse(Set())
        Mail(mailReceiver._1.id, mailReceiver._1.authorEmail, mailReceiver._1.subject, mailReceiver._1.body, receiver, meta)
      })
      (list - mailReceiver._1.id) + (mailReceiver._1.id -> mail)
    }).toList.map(_._2)

  def create(mail: Mail) : Future[Long] = mailDBDAO.create(DBMail(mail), mail.receiver)
  def create(mail: MailStub) : Future[Long] = mailDBDAO.create(DBMail(mail), mail.receiver)

  def all : Future[List[Mail]] =
    db.run(mailReceiver(None).result).map( result =>
      resultMapper(result)
    )

  def lookup(id: String) : Future[Option[Mail]] =
    db.run(mailReceiver(Some(id.toLong)).result).map( result =>
      resultMapper(result).headOption
    )

  def lookup(id: Long) : Future[Option[Mail]] =
    db.run(mailReceiver(Some(id)).result).map( result =>
      resultMapper(result).headOption
    )

  /**
    * Updates the referenced mail object and returns the new object if the update was successful.
    *
    * @author Johann Sell
    * @param id Long references the Mail
    * @return the updated mail
    */
  def send(id: Long) : Future[Option[Mail]] =
    mailDBDAO.send(id).flatMap(_ match {
      case Some(updatedId) =>
        db.run(mailReceiver(Some(updatedId)).result).map( result =>
          resultMapper(result).headOption
        )
      case None => Future.successful(None)
    })
 }