name := "akka-http-health"

organization := "io.github.lhotari"

version := "1.0.2"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", scalaVersion.value)

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaHttpVersion = "10.0.3"
  val scalaTestVersion = "3.0.1"
  val mockitoVersion = "1.10.19"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.mockito" % "mockito-all" % mockitoVersion % "test",
    "com.google.guava" % "guava" % "21.0" % "test",
    "commons-io" % "commons-io" % "2.5" % "test"
  )
}

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
publishArtifact in Test := false
bintrayRepository := "releases"
bintrayVcsUrl := Some("https://github.com/lhotari/akka-http-health")
