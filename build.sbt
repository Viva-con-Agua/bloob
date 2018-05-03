import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

scalaVersion in ThisBuild := "2.12.5"

crossScalaVersions := Seq("2.11.12", "2.12.5")
val silhouetteVersion = "5.0.2"

def gatlingVersion(scalaBinVer: String): String = scalaBinVer match {
  case "2.11" => "2.2.5"
  case "2.12" => "2.3.0"
}

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
resolvers += "MVN Repository" at "https://mvnrepository.com/artifact/"
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

libraryDependencies += ws
libraryDependencies += guice
libraryDependencies += ehcache
libraryDependencies += "org.joda" % "joda-convert" % "1.9.2"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "4.11"

libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.16"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.1"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion(scalaBinaryVersion.value) % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % gatlingVersion(scalaBinaryVersion.value) % Test
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.45"
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1"
)
libraryDependencies += "com.typesafe.play" %% "play-mailer" % "6.0.1"
libraryDependencies += "com.typesafe.play" %% "play-mailer-guice" % "6.0.1"
libraryDependencies += "org.jsoup" % "jsoup" % "1.11.2"
libraryDependencies += "com.atlassian.jwt" % "jwt-core" % "1.6.1"
libraryDependencies += "com.atlassian.jwt" % "jwt-api" % "1.6.1"

libraryDependencies += "com.iheart" %% "ficus" % "1.4.3"

libraryDependencies += "org.vivaconagua" %% "play2-oauth-client" % "0.2.0"
// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """Bloob""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// Documentation for this project:
//    sbt "project docs" "~ paradox"
//    open docs/target/paradox/site/index.html
lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin).
  settings(
    paradoxProperties += ("download_url" -> "https://example.lightbend.com/v1/download/play-rest-api")
  )
