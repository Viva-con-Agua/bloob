package utils

import javax.inject.Inject
import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.User
import daos.UserDAO
import play.api.Configuration

import scala.concurrent.Future

/**
  * A custom user service which relies on the previous defined `User`.
  */
class UserService @Inject()(userDAO: UserDAO, conf : Configuration) extends IdentityService[User] {

  private val dropsProviderID = conf.getString("drops.oauth2.providerID").get

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = loginInfo.providerID match {
    case this.dropsProviderID => userDAO.findByUUID(UUID.fromString(loginInfo.providerKey))
    case _ => Future.successful(None)
  }
}