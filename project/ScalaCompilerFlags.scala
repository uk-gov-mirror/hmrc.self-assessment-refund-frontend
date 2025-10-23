object ScalaCompilerFlags {

  val scalaCompilerOptions: Seq[String] = Seq(
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:strictEquality",
    "-Ykind-projector",
    // required in place of silencer plugin
    "-Wconf:msg=unused\\simport&src=html/.*:s",
    "-Wconf:msg=unused\\simport&src=views/.*:s",
    "-Wconf:src=routes/.*:s"
  )

  val strictScalaCompilerOptions: Seq[String] = Seq(
    "-Xfatal-warnings",
    "-Wunused:implicits",
    "-Wunused:imports",
    "-Wunused:locals",
    "-Wunused:params",
    "-Wunused:privates",
    "-Wvalue-discard",
    "-deprecation",
    "-feature",
    "-unchecked"
  )
}
