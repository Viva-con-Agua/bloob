package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Represents an e-mail.
  *
  * @author Johann Sell
  * @param author references the user that has draft the e-mail and initiated the sending.
  * @param subject of the e-mail.
  * @param body of the e-mail (Text or HTML).
  * @param receiver list of references to receiving users.
  */
case class Mail(author: String, subject: String, body: String, receiver: Set[String], meta: MailMeta) {
  override def equals(o: scala.Any): Boolean = o match {
    case m: Mail => m.author == this.author && m.subject == this.subject && m.body == this.body &&
      m.receiver.foldLeft(true)(
        (res, mReceiver) => res && this.receiver.contains(mReceiver)
      ) &&
      this.receiver.foldLeft(true)(
        (res,thisReceiver) => res && m.receiver.contains(thisReceiver)
      )
    case _ => false
  }
}

object Mail {

  //import models.MailMeta._

  def apply(author: String, subject: String, body: String, receiver: Set[String]) : Mail =
    Mail(author, subject, body, receiver, MailMeta(None, System.currentTimeMillis(), None))

  def apply(author: String, subject: String, body: String, receiver: Set[String], sendingAddress : String) : Mail =
    Mail(author, subject, body, receiver, MailMeta(Some(sendingAddress), System.currentTimeMillis(), None))

  def apply(author: String, subject: String, body: String, receiver: Set[String], sendingAddress : String, created: Long, sending: Boolean) : Mail =
    Mail(author, subject, body, receiver, MailMeta(Some(sendingAddress), created, (if (sending)  Some(System.currentTimeMillis()) else None)))

  implicit val mailWrites: Writes[Mail] = (
    (JsPath \ "author").write[String] and
      (JsPath \ "subject").write[String] and
      (JsPath \ "body").write[String] and
      (JsPath \ "receiver").write[Set[String]] and
      (JsPath \ "meta").write[MailMeta]
    )(unlift(Mail.unapply))

  implicit val mailReads: Reads[Mail] = (
    (JsPath \ "author").read[String] and
      (JsPath \ "subject").read[String] and
      (JsPath \ "body").read[String] and
      (JsPath \ "receiver").read[Set[String]] and
      (JsPath \ "meta").read[MailMeta]
    )((author, subject, body, receiver, meta) => Mail(author, subject, body, receiver, meta))

}

/**
  * Represents all meta information about sent e-mails. It's important to note, that we cannot use the users e-mail
  * address, because we have no access to all e-mail providers SMTP server. So, we have to use only Viva con Agua e-mail
  * addresses. Default should be a no-reply address of Viva con Agua.
  *
  * @author Johann Sell
  * @param sendingAddress the e-mail address used to send the e-mail.
  * @param created date of creation of the e-mail.
  * @param sent date of the sending the e-mail.
  */
case class MailMeta(sendingAddress: Option[String], created: Long, sent: Option[Long])

object MailMeta {

  implicit val mailMetaWrites: Writes[MailMeta] = (
    (JsPath \ "sendingAddress").writeNullable[String] and
      (JsPath \ "created").write[Long] and
      (JsPath \ "sent").writeNullable[Long]
    )(unlift(MailMeta.unapply))

  implicit val mailMetaReads: Reads[MailMeta] = (
    (JsPath \ "sendingAddress").readNullable[String] and
      (JsPath \ "created").read[Long] and
      (JsPath \ "sent").readNullable[Long]
    )(MailMeta.apply _)
}