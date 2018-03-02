package mariadb

import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import models.{Mail, MailStub}

private[mariadb] case class DBMail(id: Long, authorEmail: String, subject: String, body: String, metaSendingAddress: Option[String], metaCreated: Long, metaSent: Option[Long])
private[mariadb] object DBMail extends Function7[Long, String, String, String, Option[String], Long, Option[Long], DBMail] {
  def apply(mail: Mail): DBMail = DBMail(mail.id, mail.author, mail.subject, mail.body, mail.meta.sendingAddress, mail.meta.created, mail.meta.sent)
  def apply(mail: MailStub) : DBMail = DBMail(0, mail.author, mail.subject, mail.body, mail.meta.sendingAddress, mail.meta.created, mail.meta.sent)
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
  private[mariadb] class MailTable(tag: Tag) extends Table[DBMail](tag, "mails") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The user column */
    def author_email = column[String]("author_email")

    /** The subject column */
    def subject = column[String]("subject")

    /** The body column */
    def body = column[String]("body")

    /** The meta_sending_address column */
    def meta_sending_address = column[Option[String]]("meta_sending_address")

    /** The meta_created column */
    def meta_created = column[Long]("meta_created")

    /** The meta_sent column */
    def meta_sent = column[Option[Long]]("meta_sent")

    /**
      * This is the tables default "projection".
      *
      * It defines how the columns are converted to and from the Mail object.
      *
      * In this case, we are simply passing the id, name and page parameters to the Mail case classes
      * apply and unapply methods.
      */
    def * = (id, author_email, subject, body, meta_sending_address, meta_created, meta_sent) <> (DBMail.tupled, DBMail.unapply)
  }

  /**
    * The starting point for all queries on the mail table.
    */
  private[mariadb] val mails = TableQuery[MailTable]

  private def _findById(id: Long): DBIO[Option[DBMail]] =
    mails.filter(_.id === id).result.headOption

  def findById(id: Long): Future[Option[DBMail]] =
    db.run(_findById(id))

  def all: Future[List[DBMail]] =
    db.run(mails.to[List].result)

  def create(dbMail: DBMail, receiver: Set[String]): Future[Long] = {
    val interaction = for {
      m <- mails returning mails.map(_.id) += dbMail
      _ <- DBIO.sequence(receiver.toList.map((r) => receiverDAO.insert(DBReceiver(r, m))))
    } yield m
    db.run(interaction.transactionally)
  }

  /**
    * Set the mails meta attribute sent to the current time.
    *
    * @author Johann Sell
    * @param id Long references the mail
    * @return the reference id to the updated mail object
    */
  def send(id: Long) : Future[Option[Long]] = db.run {
    val q = for { m <- mails if m.id === id } yield m.meta_sent
    q.update(Some(System.currentTimeMillis())).map {
      case 0 => None
      case _ => Some(id)
    }
  }

}