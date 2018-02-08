resolvers += Classpaths.sbtPluginReleases

// Plugin for code coverage results
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// Plugin for checking code style:
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// for coverage testing
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.2")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.1")
