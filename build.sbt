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
  publishTo := sonatypePublishTo.value,
  sonatypeProjectHosting := Some(GitHubHosting("leonardehrenfried", "amqp-scala", "mail@leonard.io")),
  developers := List(Developer("platy","Mike Bush", "platy@example.com", url("https://github.com/platy")),
                     Developer("leonardehrenfried", "Leonard Ehrenfried", "mail@leonard.io", url("https://leonard.io"))),
  publishArtifact in Test := false,
  publishArtifact in IntegrationTest := false,
  pomIncludeRepository := { _ => false })

def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val prefix = out.ref.dropV.value
  val rev    = out.commitSuffix.mkString("+", "-", "")
  val dirty  = out.dirtySuffix.value

  val ver = (rev, dirty) match {
    case ("", "") =>
      prefix
    case (_, _) =>
      // (version)+(distance)-(rev)
      prefix + rev + dirty
  }
  val dynamicVersion = if (out.hasNoTags()) s"0.0.0-${out.version}$dirty" else ver
  val isSnapshot     = out.isSnapshot() || out.hasNoTags()
  if (isSnapshot) s"$dynamicVersion-SNAPSHOT" else dynamicVersion
}

def fallbackVersion(d: java.util.Date): String = s"0.0.0-${sbtdynver.DynVer timestamp d}"

inThisBuild(
  List(
    version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value)),
    dynver := {
      val d = new java.util.Date
      sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
    }
  ))
