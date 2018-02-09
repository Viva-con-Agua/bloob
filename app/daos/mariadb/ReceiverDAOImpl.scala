package mariadb

import play.api.db.slick.DatabaseConfigProvider
import slick.dbio
import slick.dbio.Effect.Read
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


private[mariadb] case class DBReceiver(id: Long, userEmail: String, mail_id: Long)
private[mariadb] object DBReceiver extends Function3[Long, String, Long, DBReceiver] {
  def apply(receiver: String, mailID: Long) : DBReceiver = DBReceiver(0, receiver, mailID)
}

@Singleton
class ReceiverDAOImpl @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  private[mariadb] class ReceiverTable(tag: Tag) extends Table[DBReceiver](tag, "receiver") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def user_email = column[String]("user_email")
    def mail_id = column[Long]("mail_id")

    def * = (id, user_email, mail_id) <> (DBReceiver.tupled, DBReceiver.unapply)
    //def ? = (id.?, user_id.?, mail_id.?).shaped.<>({ r => import r._; _1.map(_ => DBReceiver.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  private[mariadb] val receiver = TableQuery[ReceiverTable]

  def findById(id: Long): Future[DBReceiver] =
    db.run(receiver.filter(_.id === id).result.head)

  def findByMailId(mailId: Long): Future[List[DBReceiver]] =
    db.run(receiver.filter(_.mail_id === mailId).to[List].result)

  def all(): DBIO[Seq[DBReceiver]] =
    receiver.result

  def insert(dbReceiver: DBReceiver): DBIO[Long] =
    receiver returning receiver.map(_.id) += dbReceiver
}