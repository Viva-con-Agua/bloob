package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
  implicit val mailMetaFormat = Json.format[MailMeta]
}

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

  def apply(author: String, subject: String, body: String, receiver: Set[String]) : Mail =
    Mail(author, subject, body, receiver, MailMeta(None, System.currentTimeMillis(), None))

  def apply(author: String, subject: String, body: String, receiver: Set[String], sendingAddress : String) : Mail =
    Mail(author, subject, body, receiver, MailMeta(Some(sendingAddress), System.currentTimeMillis(), None))

  def apply(author: String, subject: String, body: String, receiver: Set[String], sendingAddress : String, created: Long, sending: Boolean) : Mail =
    Mail(author, subject, body, receiver, MailMeta(Some(sendingAddress), created, (if (sending)  Some(System.currentTimeMillis()) else None)))

  implicit val mailFormat = Json.format[Mail]
}