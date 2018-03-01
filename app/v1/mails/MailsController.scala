package v1.mails

import javax.inject.Inject

import play.api.Logger
import play.api.data.Form
//import play.api.libs.json.Json
import play.api.libs.json.{Json, JsError, JsValue}
import play.api.libs.streams.ActorFlow
//import play.api.libs.json.Reads._
//import play.api.libs.functional.syntax._
import play.api.mvc._

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

import models.Mail
import daos.MailDAO

/**
  * Takes HTTP requests and produces JSON.
  */
class MailsController @Inject()(cc: WSControllerComponents, mailDAO: MailDAO)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer)
    extends WSBaseController(cc) {

  private val logger = Logger(getClass)

  def index: Action[AnyContent] = WSAction.async { implicit request =>
    logger.trace("index: ")
    mailDAO.all.map { mails =>
      Ok(Json.toJson(mails))
    }
  }

  def show(id: String): Action[AnyContent] = WSAction.async { implicit request =>
    logger.trace(s"show: id = $id")
    mailDAO.lookup(id).map { _ match {
      case Some(mail) => Ok(Json.toJson(mail))
      case None => NotFound(Json.toJson("Found no mail matching given id " + id))
    }}
  }

  def create = WSAction(parse.json).async { implicit request =>
    logger.trace("create: json = " + request.body)
    val mailForm = request.body.validate[Mail]
    mailForm.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" ->"Error", "message" -> JsError.toJson(errors))))
      },
      mailObject => {
        mailDAO.create(mailObject).map { mail =>
          Created(Json.toJson(mail))
        }
      }
    )
  }

  def createWS = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef { out =>
      MailWebSocketActor.props(out, mailDAO)
    }
  }
}
