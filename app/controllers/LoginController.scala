package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Silhouette,LoginEvent}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import utils.UserService
import models.AccessToken

import scala.concurrent.ExecutionContext.Implicits.global
import utils.auth.SessionEnv
import utils.auth.DropsSocialProfileBuilder

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.ws._
import play.api._

import play.api.libs.json._

import play.api.cache.CacheApi
import play.api.i18n.{ I18nSupport, Messages }

class LoginController @Inject()(
                                 ws: WSClient,
                                 conf : Configuration,
                                 cc: ControllerComponents,
                                 silhouette: Silhouette[SessionEnv],
                                 userService: UserService,
                                 authInfoRepository: AuthInfoRepository,
                                 socialProviderRegistry: SocialProviderRegistry,
                                 cache: CacheApi
                               ) extends AbstractController(cc) with I18nSupport {

  val logger: Logger = Logger(this.getClass())
  val dropsLogin = conf.getString("drops.url.base").get + conf.getString("drops.url.login").get

  /**
    * Authenticates a user against a social provider.
    *
    * @param provider The ID of the provider to authenticate against.
    * @return The result to display.
    */
  def authenticate(provider: String) = Action.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with DropsSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            profile <- p.retrieveProfile(authInfo)
            user <- userService.retrieve(profile.loginInfo)
            authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
            authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
            token <- silhouette.env.authenticatorService.init(authenticator)
            result <- silhouette.env.authenticatorService.embed(token, Redirect(routes.HomeController.index))
          } yield {
            user match {
              case Some(u) => {
                silhouette.env.eventBus.publish(LoginEvent(u, request))
                result
              }
              case _ => {
                logger.error("Unexpected provider error", new ProviderException(s"Found no user for given LoginInfo key $profile.loginInfo.providerKey"))
                Redirect(dropsLogin).flashing("error" -> Messages("could.not.authenticate"))
              }
            }
          }
        }
      case _ => {
        logger.error("Unexpected provider error", new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
        Future.successful(Redirect(dropsLogin).flashing("error" -> Messages("could.not.authenticate")))
      }
    })
  }

  def login = Action {
    val url = conf.getString("drops.url.base").get + conf.getString("drops.url.code").get +
      conf.getString("drops.client_id").get
    Redirect(url)
  }

  def receiveCode(code: String) = Action.async {
    val url = conf.getString("drops.url.base").get + conf.getString("drops.url.accessToken").get
    val clientId = conf.getString("drops.client_id").get
    val clientSecret = conf.getString("drops.client_secret").get

    val accessToken = ws.url(url).withQueryString(
      "grant_type" -> "authorization_code",
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "code" -> code,
      "redirect_uri" -> "http://localhost:8080/endpoint?code="
    ).get().map(response => response.status match {
      case 200 => AccessToken(response.json)
      case _ => println(response.status); throw new Exception // Todo: throw meaningful exception considering the returned error message and status code!
    })

    accessToken.flatMap(token => {
      val url = conf.getString("drops.url.base").get + conf.getString("drops.url.profile").get

      ws.url(url).withQueryString(
        "access_token" -> token.content
      ).get().map(response => response.status match {
        case 200 => Ok(Json.obj("status" -> "success", "code" -> code, "token" -> token.content, "user" -> response.json))
        case _ => Ok(Json.obj("status" -> "error", "code" -> code, "token" -> token.content, "response-status" -> response.status))
      })
    })
  }
}