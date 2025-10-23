import sbt.Def
import scoverage.ScoverageKeys

object ScoverageSettings {

  lazy val coverageExcludedFiles: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    ".*handlers.*",
    ".*components.*",
    ".*Routes.*",
    ".*viewmodels.govuk.*",
    ".*TestOnly*"
  )

  lazy val coverageExcludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*BuildInfo.*",
    "Reverse.*",
    "app.Routes.*",
    "prod.*",
    "testOnlyDoNotUseInProd.*",
    "forms.*",
    ".*views.html.*",
    ".*testonly.*",
    "config.*"
  )

  lazy val settings: Seq[Def.Setting[?]] =
    Seq(
      ScoverageKeys.coverageExcludedPackages := coverageExcludedPackages.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 80.00,
      ScoverageKeys.coverageExcludedFiles := coverageExcludedFiles.mkString(";"),
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )
}
