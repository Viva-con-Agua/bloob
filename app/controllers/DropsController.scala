package controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import org.vivaconagua.play2OauthClient.silhouette.CookieEnv
import org.vivaconagua.play2OauthClient.controller.DropsLoginController
import org.vivaconagua.play2OauthClient.silhouette.UserService
import concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.ws._
import play.api._
import play.api.cache.CacheApi

class DropsController @Inject()(
                                ws: WSClient,
                                override val conf : Configuration,
                                cc: ControllerComponents,
                                override val silhouette: Silhouette[CookieEnv],
                                override val userService: UserService,
                                override val authInfoRepository: AuthInfoRepository,
                                override val socialProviderRegistry: SocialProviderRegistry,
                                cache: CacheApi
                              ) extends AbstractController(cc) with DropsLoginController {
  override val defaultRedirectUrl = routes.HomeController.index.url
}
