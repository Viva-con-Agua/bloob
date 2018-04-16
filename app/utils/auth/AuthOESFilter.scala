package utils.auth

import javax.inject._

import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import utils.auth.CookieEnv
import utils.auth.AuthOESHandler.IsLoggedOut

import models.User

import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logger

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.stream.Materializer

import scala.concurrent.{Future,ExecutionContext}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AuthOESFilter @Inject() (authOES: AuthOES, silhouette: Silhouette[CookieEnv], bodyParsers: PlayBodyParsers)(implicit override val mat: Materializer) extends Filter {

  val logger: Logger = Logger(this.getClass())

  override def apply(next: RequestHeader => Future[Result])(
    request: RequestHeader): Future[Result] = {

    implicit val timeout = Timeout(5 second)
    logger.debug("Auth OES filter called!")

    // As the body of request can't be parsed twice in Play we should force
    // to parse empty body for UserAwareAction
    val action = silhouette.UserAwareAction.async(bodyParsers.empty) { r => r.identity match {
      case Some(user) => r.authenticator match {
        case Some(authenticator) => authOES.sendActor ? IsLoggedOut(user) flatMap {
          _ match {
            case true => {
              logger.debug("User and Authenticator detected, but user is logged out!")
              implicit val reqHeader = r
              val result = Redirect(r.uri)
              silhouette.env.eventBus.publish(LogoutEvent(user, r))
              silhouette.env.authenticatorService.discard(authenticator, result)
            }
            case false => next(request)
          }
        }
        case None => next(request)
      }
      case None => next(request)
    }}

    action(request).run
  }

//  override def mat = mat
}