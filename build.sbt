import scala.xml.NodeSeq
import xerial.sbt.Sonatype._

val commonSettings = Seq(
  organization := "io.leonard",
  scalacOptions ++= Seq("-Xfatal-warnings", "-deprecation", "-feature"),
  scalacOptions in Test --= Seq("-Xfatal-warnings") // mocking triggers a deprecated warning
)

lazy val `rabbitmq-scala-client` = project.in(file(".")).
  configs(IntegrationTest).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.3" % "it,test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "it,test",
      "com.rabbitmq" % "amqp-client" % "4.2.0"
    )
  ).
  settings(Defaults.itSettings: _*).
  settings(parallelExecution in IntegrationTest := false).
  settings(publishSettings: _*).
  dependsOn(`amqp-embedded-test` % "it,test")

lazy val `amqp-embedded-test` = project.
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "org.apache.qpid" % "qpid-broker" % "0.32"))

lazy val publishSettings = mavenCentralPublishSettings ++
  organizationSettings ++
  Seq(
    name := "AMQP Scala Client",
    licenses := Seq("The MIT License" -> url("http://ithings4u.mit-license.org/")),
    description := "Scala wrapper for rabbitmq-java-client",
  )

lazy val organizationSettings = Seq(organizationName := "Relayr",
  organizationHomepage := Some(url("https://relayr.io")))

lazy val mavenCentralPublishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  sonatypeProjectHosting := Some(GitHubHosting("leonardehrenfried", "amqp-scala", "mail@leonard.io")),
  developers := List(Developer("platy","Mike Bush", "platy@example.com", url("https://github.com/platy")),
                     Developer("leonardehrenfried", "Leonard Ehrenfried", "mail@leonard.io", url("https://leonard.io"))),
  publishArtifact in Test := false,
  publishArtifact in IntegrationTest := false,
  pomIncludeRepository := { _ => false })

lazy val extraPomXml: NodeSeq =
  <developers>
    <developer>
      <id>platy</id>
      <name>Mike Bush</name>
    </developer>
  </developers>
