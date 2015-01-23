import scalariform.formatter.preferences._

lazy val commonSettings = Seq(
  scalaVersion := "2.11.5",
  organization := "io.relayr"
)

lazy val root = Project("rabbitmq-scala-client", file(".")).
  configs(IntegrationTest).
  settings(commonSettings: _*).
  settings(Defaults.itSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "org.apache.qpid" % "qpid-broker" % "0.30" % "it",
      "org.scalatest" %% "scalatest" % "2.2.1" % "it,test",
      "com.rabbitmq" % "amqp-client" % "3.4.2")
  )

scalariformSettings ++ Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(RewriteArrowSymbols, true)
)
