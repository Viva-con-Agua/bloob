package utils

import play.api.libs.mailer._
import javax.inject.Inject
import scala.concurrent.Future
import org.jsoup.Jsoup

import models.Mail
import daos.MailDAO

import scala.concurrent.ExecutionContext.Implicits.global

class MailerService @Inject() (mailerClient: MailerClient, mailDAO : MailDAO) {

  private val defaultSender = "no-reply@vivaconagua.org"

  def send(id: Long) : Future[Option[Mail]]  = {
    mailDAO.lookup(id).flatMap(_ match {
      case Some(savedMail) => this.send(savedMail)
      case None => Future.successful(None)
    })
  }

  def send(mail: Mail) : Future[Option[Mail]] = {
    val email = Email(
      mail.subject,
      mail.meta.sendingAddress.map(toSender( _ )).getOrElse(toSender( defaultSender )), // TODO: If author is complete user, add possibility to send from users vivaconagua address
      mail.receiver.toSeq, // TODO: If receiver are complete users, adopt schema: "Miss TO <to@email.com>"
      // sends text, HTML or both...
      bodyText = Some(Jsoup.parse(mail.body).text),//Some(scala.xml.XML.loadString(mail.body).text), // TODO: Does this function respects line breaks, etc?
      bodyHtml = Some(mail.body)
    )
    mailerClient.send(email)
    mailDAO.send(mail.id)
  }

  private def toSender(email: String) : String = "Viva con Agua FROM <" + email + ">"

}