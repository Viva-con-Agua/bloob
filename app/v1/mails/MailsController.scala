package v1.mails

import javax.inject.Inject

import play.api.Logger
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

import models.Mail
import daos.MailDAO

case class MailFormInput(
                          author: String,
                          subject: String,
                          body: String,
                          receiver: List[String],
                          sendingAddress: String,
                          sending: Boolean,
                          created: Long
                        ) {
  def toMail : Mail = Mail(author, subject, body, receiver.toSet, sendingAddress, created, sending)
}

/**
  * Takes HTTP requests and produces JSON.
  */
class MailsController @Inject()(cc: WSControllerComponents, mailDAO: MailDAO)(implicit ec: ExecutionContext)
    extends WSBaseController(cc) {

  private val logger = Logger(getClass)

  private val form: Form[MailFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "author" -> nonEmptyText,
        "subject" -> nonEmptyText,
        "body" -> nonEmptyText,
        "receiver" -> list(email),
        "sendingAddress" -> text,
        "sending" -> boolean,
        "created" -> longNumber
      )(MailFormInput.apply)(MailFormInput.unapply)
    )
  }

  def index: Action[AnyContent] = WSAction.async { implicit request =>
    logger.trace("index: ")
    mailDAO.all.map { mails =>
      Ok(Json.toJson(mails))
    }
  }

  def process: Action[AnyContent] = WSAction.async { implicit request =>
    logger.trace("process: ")
    processJsonPost()
  }

  def show(id: String): Action[AnyContent] = WSAction.async { implicit request =>
    logger.trace(s"show: id = $id")
    mailDAO.lookup(id).map { _ match {
      case Some(mail) => Ok(Json.toJson(mail))
      case None => NotFound(Json.toJson("Found no mail matching given id " + id))
    }}
  }

  private def processJsonPost[A]()(implicit request: WSRequest[A]): Future[Result] = {
    def failure(badForm: Form[MailFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: MailFormInput) = {
      mailDAO.create(input.toMail).map { mail =>
        Created(Json.toJson(mail))//.withHeaders(LOCATION -> post.link)
      }
    }

    form.bindFromRequest().fold(failure, success)
  }
}
