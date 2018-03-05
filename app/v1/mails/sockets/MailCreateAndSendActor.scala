package v1.mails.sockets

import akka.actor._
import play.api.libs.json.{Json,JsSuccess,JsError,JsValue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.{Mail, MailStub}
import daos.MailDAO
import utils.MailerService

object MailCreateAndSendActor {
  def props(out: ActorRef, mailDAO: MailDAO, mailer: MailerService) = Props(new MailCreateAndSendActor(out, mailDAO, mailer))
}

class MailCreateAndSendActor(out: ActorRef, mailDAO: MailDAO, mailer: MailerService) extends Actor {

  def receive = {
    case mail: JsValue => mail.validate[MailStub] match {
      case m: JsSuccess[MailStub] => mailDAO.create(m.get).flatMap(_ match {
        case mailID : Long => mailer.send(mailID).map(_ match {
          case Some(sentMail) => out ! Json.toJson(sentMail)
          case None => out ! Json.obj(
            "status" -> "error",
            "code" -> 500,
            "message" -> "Could not send the mail!"
          )
        })
        case _ => Future.successful(out ! Json.obj(
          "status" -> "error",
          "code" -> 500,
          "message" -> "Could not save the mail!"
        ))})
      case e: JsError => Future.successful(out ! JsError.toJson(e))
    }
  }
}