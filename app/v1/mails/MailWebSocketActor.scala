package v1.mails

import akka.actor._
import play.api.libs.json.{Json,JsSuccess,JsError,JsValue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.Mail
import daos.MailDAO

object MailWebSocketActor {
  def props(out: ActorRef, mailDAO: MailDAO) = Props(new MailWebSocketActor(out, mailDAO))
}

class MailWebSocketActor(out: ActorRef, mailDAO: MailDAO) extends Actor {

  def receive = {
    case mail: JsValue => mail.validate[Mail] match {
      case m: JsSuccess[Mail] => mailDAO.create(m.get).map(_ => {
        out ! Json.toJson(m.get)
      })
      case e: JsError => Future.successful(out ! JsError.toJson(e))
    }
  }

}