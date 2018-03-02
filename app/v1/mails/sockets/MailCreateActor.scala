package v1.mails.sockets

import akka.actor._
import play.api.libs.json.{Json,JsSuccess,JsError,JsValue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.{Mail, MailStub}
import daos.MailDAO

object MailCreateActor {
  def props(out: ActorRef, mailDAO: MailDAO) = Props(new MailCreateActor(out, mailDAO))
}

class MailCreateActor(out: ActorRef, mailDAO: MailDAO) extends Actor {

  def receive = {
    case mail: JsValue => mail.validate[MailStub] match {
      case m: JsSuccess[MailStub] => mailDAO.create(m.get).map(_ => {
        out ! Json.toJson(m.get)
      })
      case e: JsError => Future.successful(out ! JsError.toJson(e))
    }
  }

}