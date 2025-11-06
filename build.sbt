inThisBuild(
  Seq(
    scalaVersion := "3.3.7"
  )
)
// publish config
inThisBuild(
  Seq(
    Test / publishArtifact := false,
    // pomIncludeRepository := (_ => false),
    organization := "org.hyperledger.identus",
    homepage := Some(
      url("https://github.com/hyperledger-identus/prism-vdr-driver")
    ),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
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
    versionScheme := Some(
      "early-semver"
    ) // https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme
  )
)

/** Versions */
lazy val V = new {
  val scalaDID = "0.1.0-M30"

  val identusVDR = "0.2.1"

  val munit = "1.2.1"

  val zio = "2.1.5"
  val zioJson = "0.7.42"
  val zioHttp = "3.3.3"
  val zioConfig = "4.0.4"
  val zioLogging = "2.2.4"
  val zioSl4j = "2.2.2"
  val logback = "1.5.6"
  val logstash = "7.4"
  val munitZio = "0.4.0"
  val zioTest = "2.1.5"
  val zioTestSbt = "2.1.5"
  val zioTestMagnolia = "2.1.5"
}

/** Dependencies */
lazy val D = new {
  val scalaDID = Def.setting("app.fmgp" %% "did" % V.scalaDID)
  val scalaDIDPrism = Def.setting("app.fmgp" %% "did-method-prism" % V.scalaDID)
  val identusVDR =
    Def.setting("org.hyperledger.identus" % "vdr" % V.identusVDR)

  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val zioJson = Def.setting("dev.zio" %% "zio-json" % V.zioJson)

  val zioHttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp)
  val zioConfig = Def.setting("dev.zio" %% "zio-config" % V.zioConfig)
  val zioConfigMagnolia = Def.setting(
    "dev.zio" %% "zio-config-magnolia" % V.zioConfig
  ) // For deriveConfig
  val zioConfigTypesafe =
    Def.setting("dev.zio" %% "zio-config-typesafe" % V.zioConfig) // For HOCON
  val zioLogging = Def.setting("dev.zio" %% "zio-logging" % V.zioLogging)
  val zioLoggingSl4j = Def.setting("dev.zio" %% "zio-logging-slf4j" % V.zioSl4j)
  val logback = Def.setting("ch.qos.logback" % "logback-classic" % V.logback)
  val logstash = Def.setting(
    "net.logstash.logback" % "logstash-logback-encoder" % V.logstash
  )

  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)

  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio =
    Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)
  val zioTest = Def.setting("dev.zio" %% "zio-test" % V.zioTest % Test)
  val zioTestSbt =
    Def.setting("dev.zio" %% "zio-test-sbt" % V.zioTestSbt % Test)
  val zioTestMagnolia =
    Def.setting("dev.zio" %% "zio-test-magnolia" % V.zioTestMagnolia % Test)

}

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
          // TODO "-Yexplicit-nulls",
          // "-Ysafe-init", // https://dotty.epfl.ch/docs/reference/other-new-features/safe-initialization.html
          "-language:implicitConversions" // we can use with the flag '-feature'
        )
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "prism-vdr-driver",
    description := "PRISM VDR Driver",
    libraryDependencies ++= Seq(
      // D.scalaDID.value,
      D.scalaDIDPrism.value,
      D.identusVDR.value,
      D.munit.value,
      D.munitZio.value
    )
  )

lazy val demo = project
  .in(file("demo"))
  .dependsOn(root)
  .settings(
    name := "prism-vdr-demo",
    description := "PRISM VDR Driver Demo Examples",
    libraryDependencies ++= Seq(
      D.scalaDIDPrism.value,
      D.zio.value
    ),
    publish / skip := true
  )
