package daos.drops

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
        case 404 => {
          logger.info("Requested user " + uuid + " not found!")
          None
        }
        case _ => throw UserDAONetworkException(response.json)
      })
      case "POST" => url.addHttpHeaders("Content-Type" -> "application/json", "Accept" -> "application/json").post(Json.obj()).map(response => response.status match {
        case 200 => Some(toUser(response.json))
        case 404 => {
          logger.info("Requested user " + uuid + " not found!")
          None
        }
        case _ => throw UserDAONetworkException(response.json)
      })
      case _ => throw UserDAOHTTPMethodException(restMethod)
    }
  }

  private def toUser(json: JsValue) : User =
    User(
      uuid = (json \ "id").as[UUID],
      email = (json \\ "email").map(_.as[String]).toSet
    )
}

case class UserDAOHTTPMethodException(method: String, cause: Throwable = null) extends
  Exception("HTTP method " + method + " is not supported.", cause)

case class UserDAONetworkException(status: Int, typ: String, msg: String, cause: Throwable = null) extends
  Exception(msg, cause)

object UserDAONetworkException {
  def apply(dropsResponse: JsValue) : UserDAONetworkException =
    UserDAONetworkException(
      status = (dropsResponse \ "code").as[Int],
      typ = (dropsResponse \ "type").as[String],
      msg = (dropsResponse \ "msg").as[String]
    )
}