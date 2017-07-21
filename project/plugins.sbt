resolvers += Classpaths.sbtPluginReleases

// Plugin for code coverage results
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

// Plugin for code formatting:
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.7.0")

// Plugin for checking code style:
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.9.0")

// for coverage testing
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

// for release signing (requires a sonatype.sbt credentials file in home .sbt)
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")
