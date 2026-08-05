addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1")

// Utils Buildinfo - https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

// CI - https://github.com/rtimush/sbt-updates/tags
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.7.0") // sbt> dependencyUpdates

// TEST COVERAGE - https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4") // Needs scala version 3.2.2

// PUBLISH
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.0")
// addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.17")
// addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1") //https://github.com/sbt/sbt-pgp#sbt-pgp

// Deploy demo - https://github.com/sbt/sbt-assembly/tags
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
// Removed sbt-revolver/sbt-gzip: not published for sbt 2.x and unused in this build.

// To debug what the job sends to https://github.com/FabioPinheiro/scala-did/security/dependabot
// See file in .github/workflows/sbt-dependency-submission.yml
if (sys.env.get("DEPEDABOT").isDefined) {
  println(s"Adding plugin sbt-github-dependency-submission since env DEPEDABOT is defined.")
  // The reason for this is that the plugin needs the variable to be defined. We don't want to have that requirement.
  libraryDependencies += {
    val dependency = "ch.epfl.scala" % "sbt-github-dependency-submission" % "3.1.0"
    val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
    val scalaV = (update / scalaBinaryVersion).value
    Defaults.sbtPluginExtra(dependency, sbtV, scalaV)
  }
} else libraryDependencies ++= Seq[ModuleID]()
