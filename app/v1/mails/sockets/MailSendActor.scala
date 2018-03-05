package v1.mails.sockets

import akka.actor._
import play.api.libs.json.{Json,JsSuccess,JsError,JsValue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.Mail
import utils.MailerService

object MailSendActor {
  def props(out: ActorRef, mailer: MailerService) = Props(new MailSendActor(out, mailer))
}

class MailSendActor(out: ActorRef, mailer: MailerService) extends Actor {

  def receive = {
    case mail: JsValue => mail.validate[Mail] match {
      case m: JsSuccess[Mail] => mailer.send(m.get).map(_ match {
        case Some(sentMail) => out ! Json.toJson(sentMail)
        case None => out ! Json.obj(
          "status" -> "error",
          "code" -> 500,
          "message" -> "Could not send the mail!"
      )})
      case e: JsError => Future.successful(out ! JsError.toJson(e))
    }
  }

}