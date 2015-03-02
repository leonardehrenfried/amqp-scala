resolvers += Classpaths.sbtPluginReleases

// Plugin for code coverage results
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.1")

// Plugin for code formatting:
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// Plugin for checking code style:
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.4.0")

// for coverage testing
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0.BETA1")

// for release signing (requires a sonatype.sbt credentials file in home .sbt)
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")
