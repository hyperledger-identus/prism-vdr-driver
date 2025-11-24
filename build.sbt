inThisBuild(Seq(scalaVersion := "3.3.7"))
inThisBuild(
  Seq(
    // ### https://docs.scala-lang.org/scala3/guides/migration/options-new.html
    // ### https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html
    scalacOptions ++=
      Seq("-encoding", "UTF-8") ++ // source files are in UTF-8
        Seq(
          "-deprecation", // warn about use of deprecated APIs
          "-unchecked", // warn about unchecked type parameters
          "-feature", // warn about misused language features (Note we are using 'language:implicitConversions')
          "-language:implicitConversions" // we can use with the flag '-feature'
        )
  )
)
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

/** Versions */
lazy val V = new {
  val scalaDID = "0.1.0-M32"
  val reactivemongo = "1.1.0-RC17"
  val identusVDR = "0.2.1"

  val munit = "1.2.1"
  val munitZio = "0.4.0"
  val zio = "2.1.5"
}

/** Dependencies */
lazy val D = new {
  val scalaDIDPrism = Def.setting("app.fmgp" %% "did-method-prism" % V.scalaDID)
  val reactivemongo = Def.setting("org.reactivemongo" %% "reactivemongo" % V.reactivemongo)
  val identusVDR = Def.setting("org.hyperledger.identus" % "vdr" % V.identusVDR)
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio = Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)

}

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
