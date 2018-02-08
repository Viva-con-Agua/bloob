package mariadb

import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider
import slick.dbio
import slick.dbio.Effect.Read
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import models.Mail

private[mariadb] case class DBMail(id: Long, authorEmail: String, subject: String, body: String)
private[mariadb] object DBMail extends Function4[Long, String, String, String, DBMail] {
  def apply(mail: Mail): DBMail = DBMail(0, mail.author, mail.subject, mail.body)
}

@Singleton
class DBMailDAOImpl @Inject()(receiverDAO: ReceiverDAOImpl)(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
    * Here we define the table. It will have a user, a subject, a body and a list of receiver
    */
  private class MailTable(tag: Tag) extends Table[DBMail](tag, "mails") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The user column */
    def author_email = column[String]("author_email")

    /** The subject column */
    def subject = column[String]("subject")

    /** The body column */
    def body = column[String]("body")

    /**
      * This is the tables default "projection".
      *
      * It defines how the columns are converted to and from the Mail object.
      *
      * In this case, we are simply passing the id, name and page parameters to the Mail case classes
      * apply and unapply methods.
      */
    def * = (id, author_email, subject, body) <> (DBMail.tupled, DBMail.unapply)
  }

  /**
    * The starting point for all queries on the mail table.
    */
  private val mails = TableQuery[MailTable]

  private def _findById(id: Long): DBIO[Option[DBMail]] =
    mails.filter(_.id === id).result.headOption

  def findById(id: Long): Future[Option[DBMail]] =
    db.run(_findById(id))

  def all: Future[List[DBMail]] =
    db.run(mails.to[List].result)

  def create(mail: Mail): Future[Long] = {
    val dbMail = DBMail(mail)
    val interaction = for {
      m <- mails returning mails.map(_.id) += dbMail
      _ <- DBIO.sequence(mail.receiver.toList.map((r) => receiverDAO.insert(DBReceiver(r, m))))
    } yield m
//    db.run(mails returning mails.map(_.id) += dbMail)
    db.run(interaction.transactionally)
  }

  private def addReceiver(mailId: Long, receiver: String): Future[Long] = {
    val interaction = for {
      Some(dbMail) <- _findById(mailId)
      id <- receiverDAO.insert(DBReceiver(receiver, dbMail.id))
    } yield id

    db.run(interaction.transactionally)
  }

}