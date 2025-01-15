import scoverage.ScoverageKeys

object ScoverageSettings {

  lazy val settings =
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;.*BuildInfo.*;Reverse.*;app.Routes.*;prod.*;testOnlyDoNotUseInProd.*;manualdihealth.*;forms.*;.*views.html.*;.*testonly.*;config.*;",
      ScoverageKeys.coverageMinimumStmtTotal := 80.00,
      ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;.*Routes.*;.*viewmodels.govuk.*;.*TestOnly*;",
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
}
