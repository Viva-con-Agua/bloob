package controllers

import javax.inject.Inject

import play.api.mvc._
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredAction
import utils.auth.CookieEnv
import utils.UserService

import daos.MailDAO
import models.Mail

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * A very small controller that renders a home page.
  */
class HomeController @Inject()(
                                mailDAO: MailDAO,
                                cc: ControllerComponents,
                                silhouette: Silhouette[CookieEnv],
                                userService: UserService
) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def daoTest = Action.async { implicit request => {
    val mail1 = Mail(1, "Johann", "Test Mail", "Dies ist eine Testmail!", Set("Dennis", "Jens"))
    val mail2 = Mail(2, "Dennis", "Tester Mail", "Dies ist noch eine Testmail!", Set("Johann", "Jens"), "no-reply@vivaconagua.org")
    mailDAO.create(mail1)
    mailDAO.create(mail2)
    val fromDB = mailDAO.all
    fromDB.map((mailList) => {
      if(mailList.contains(mail1) && mailList.contains(mail2)) {
        Ok("Mail found in database!")
      } else {
        Ok("Mail NOT found in database!")
      }
    })
  }}

  def userTest = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok("User: " + request.identity))
  }
}
