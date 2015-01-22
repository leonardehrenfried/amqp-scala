import scalariform.formatter.preferences._

name := "rabbitmq-scala-client"

organization := "io.relayr"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.apache.qpid" % "qpid-broker" % "0.30" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test",
  "com.rabbitmq" % "amqp-client" % "3.4.2")

scalariformSettings ++ Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(RewriteArrowSymbols, true)
)
