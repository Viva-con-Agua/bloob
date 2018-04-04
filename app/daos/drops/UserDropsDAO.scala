package drops

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import play.api.Configuration
import daos.UserDAO
import models.User
import play.api.libs.ws._
import play.api.libs.json._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global

class UserDropsDAO @Inject() (ws: WSClient, conf : Configuration) extends UserDAO {
  val logger: Logger = Logger(this.getClass())

  val restEndpoint = conf.getString("drops.url.base").get + conf.getString("drops.url.rest.user.path").get
  val restMethod = conf.getString("drops.url.rest.user.method").get

  val dropsClientId = conf.getString("drops.client_id").get
  val dropsClientSecret = conf.getString("drops.client_secret").get

  override def findByUUID(uuid: UUID) : Future[Option[User]] = {
    val url = ws.url(restEndpoint + uuid + "?client_id=" + dropsClientId + "&client_secret=" + dropsClientSecret)
    restMethod match {
      case "GET" => url.get().map(response => response.status match {
        case 200 => Some(toUser(response.json))
        case _ => logger.error("Requesting user and got response status: " + response.status); None // Todo: throw meaningful exception considering the returned error message and status code!
      })
      case "POST" => url.addHttpHeaders("Content-Type" -> "application/json", "Accept" -> "application/json").post(Json.obj()).map(response => response.status match {
        case 200 => Some(toUser(response.json))
        case _ => {
          logger.error("Requesting user and got response status: " + response.status)
          logger.error((response.json \ "error").as[String])
          None // Todo: throw meaningful exception considering the returned error message and status code!
        }
      })
      case _ => Future.successful(None) // Todo: PUT and DELETE!
    }
  }

  private def toUser(json: JsValue) : User =
    User(
      uuid = (json \ "id").as[UUID],
      email = (json \\ "email").map(_.as[String]).toSet
    )
}