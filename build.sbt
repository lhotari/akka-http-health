name := "akka-http-health"

organization := "io.github.lhotari"

version := "1.0.9"

scalaVersion := "2.13.1"

crossScalaVersions := Seq("2.11.12", "2.12.10", scalaVersion.value)

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaHttpVersion = "10.1.11"
  val akkaVersion = "2.5.29"
  val scalaCollectionCompatVersion = "2.1.4"
  val scalaTestVersion = "3.0.8"
  val mockitoVersion = "1.10.19"
  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.mockito" % "mockito-all" % mockitoVersion % "test",
    "com.google.guava" % "guava" % "21.0" % "test",
    "commons-io" % "commons-io" % "2.5" % "test"
  )
}

bintrayRepository := "releases"
bintrayVcsUrl := Some("https://github.com/lhotari/akka-http-health")
publishArtifact in Test := false

// fullfil Sonatype OSS requirements for pom
publishMavenStyle := true
pomIncludeRepository := { x => false }
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
homepage := Some(url("https://github.com/lhotari/akka-http-health"))
pomExtra := (
  <scm>
    <url>https://github.com/lhotari/akka-http-health</url>
    <connection>scm:git:https://github.com/lhotari/akka-http-health.git</connection>
  </scm>
  <developers>
    <developer>
      <id>lhotari</id>
      <name>Lari Hotari</name>
      <email>lari@hotari.net</email>
    </developer>
  </developers>
)
