import scala.xml.NodeSeq

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
    name := "RabbitMQ Scala Client",
    licenses := Seq("The MIT License" -> url("http://ithings4u.mit-license.org/")),
    homepage := Some(url("https://github.com/relayr/rabbitmq-scala-client")),
    description := "Scala wrapper for rabbitmq-java-client",
    pomExtra := extraPomXml
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
  publishArtifact in Test := false,
  publishArtifact in IntegrationTest := false,
  pomIncludeRepository := { _ => false })

lazy val extraPomXml: NodeSeq =
  <scm>
    <url>git@github.com:relayr/rabbitmq-scala-client.git</url>
    <connection>scm:git:git@github.com:relayr/rabbitmq-scala-client.git</connection>
  </scm>
  <developers>
    <developer>
      <id>platy</id>
      <name>Mike Bush</name>
    </developer>
  </developers>
