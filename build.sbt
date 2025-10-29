val strictBuilding: SettingKey[Boolean] = StrictBuilding.strictBuilding //defining here so it can be set before running sbt like `sbt 'set Global / strictBuilding := true' ...`
StrictBuilding.strictBuildingSetting

import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "self-assessment-refund-frontend"

scalaVersion  := "3.3.6"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    majorVersion              := 0,
    libraryDependencies       ++= AppDependencies.compile ++ AppDependencies.test,
    PlayKeys.playDefaultPort  := 9171,
    (Assets / pipelineStages) := Seq(gzip),
    (Compile / doc / sources) := Seq.empty,
    scalacOptions ++= ScalaCompilerFlags.scalaCompilerOptions,
    scalacOptions ++= {
      if (StrictBuilding.strictBuilding.value) ScalaCompilerFlags.strictScalaCompilerOptions else Nil
    },
  )
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.JourneyId",
      "uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber",
      "uk.gov.hmrc.selfassessmentrefundfrontend.testonly.model.StartJourneyOptions"
    )
  )
  .settings(scalafmtOnCompile := true)
  .settings(commands ++= SbtCommands.commands)
  .settings((Test / fork) := false)
  .settings(ScoverageSettings.settings *)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)
  .settings(WartRemoverSettings.wartRemoverSettings *)
