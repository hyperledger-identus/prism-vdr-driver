inThisBuild(Seq(scalaVersion := "3.3.8"))
// scalacOptions (-deprecation, -feature, -unchecked, -language:implicitConversions, -encoding)
// are sbt 2.x defaults, so they are no longer set explicitly here.

inThisBuild( // publish config
  Seq(
    Test / publishArtifact := false,
    organization := "org.hyperledger.identus",
    homepage := Some(url("https://github.com/hyperledger-identus/prism-vdr-driver")),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/hyperledger-identus/prism-vdr-driver"),
        "scm:git:git@github.com:hyperledger-identus/prism-vdr-driver.git"
      )
    ),
    developers := List(
      Developer(
        "FabioPinheiro",
        "Fabio Pinheiro",
        "fabiomgpinheiro@gmail.com",
        url("https://fmgp.app")
      )
    ),
    // updateOptions := updateOptions.value.withLatestSnapshots(false),
    versionScheme := Some("early-semver") // https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "prism-vdr-driver",
    description := "PRISM VDR Driver",
    libraryDependencies ++= Seq(
      D.scalaDIDPrism.value,
      D.reactivemongo.value,
      D.identusVDR.value,
      D.munit.value,
      D.munitZio.value,
    )
  )

lazy val demo = project
  .in(file("demo"))
  .dependsOn(root)
  .settings(
    name := "prism-vdr-demo",
    description := "PRISM VDR Driver Demo Examples",
    publish / skip := true,
    libraryDependencies += D.scalaDIDPrism.value,
  )
