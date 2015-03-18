import scala.xml.NodeSeq
import scalariform.formatter.preferences._

lazy val root = Project("rabbitmq-scala-client", file(".")).
  configs(IntegrationTest).
  settings(
    scalaVersion := "2.11.5",
    organization := "io.relayr",
    crossScalaVersions := Seq("2.10.4", "2.11.5"),
    libraryDependencies ++= Seq(
      "org.apache.qpid" % "qpid-broker" % "0.30" % "it,test", // test scope is just so that it's not included in compile scope in the published pom, may be related to https://github.com/sbt/sbt/issues/1380
      "org.scalatest" %% "scalatest" % "2.2.1" % "it,test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2.1" % "it,test",
      "com.rabbitmq" % "amqp-client" % "3.4.2")).
  settings(Defaults.itSettings: _*).
  settings(publishSettings: _*)

scalariformSettings ++ Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(RewriteArrowSymbols, true)
)

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
