import sbt.*

// Version/dependency constants; kept in `project/` for sbt 2.x / Scala 3 compatibility
// (the `new { ... }` refinement pattern used previously is unsupported there).

object V {
  val scalaDID = "0.1.0"
  val reactivemongo = "1.1.0-RC17"
  val identusVDR = "0.2.1"

  val munit = "1.3.4"
  val munitZio = "0.4.0"
  val zio = "2.1.5" // "2.1.22"
}

object D {
  val scalaDIDPrism = Def.setting("app.fmgp" %% "did-method-prism" % V.scalaDID)
  val reactivemongo = Def.setting("org.reactivemongo" %% "reactivemongo" % V.reactivemongo)
  val identusVDR = Def.setting("org.hyperledger.identus" % "vdr" % V.identusVDR)
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio = Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)
}
