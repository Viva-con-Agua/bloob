package models

import java.util.UUID
import com.mohiva.play.silhouette.api.Identity

case class User(uuid: UUID, email: Set[String]) extends Identity