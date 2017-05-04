name := "akka-http-health"

organization := "io.github.lhotari"

version := "1.0.8"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", scalaVersion.value)

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaHttpVersion = "10.0.6"
  val scalaTestVersion = "3.0.1"
  val mockitoVersion = "1.10.19"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
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
