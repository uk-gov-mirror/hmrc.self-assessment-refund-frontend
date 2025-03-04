import sbt.*

object AppDependencies {

  val bootstrapVersion = "9.11.0"

  val compile: Seq[ModuleID] = Seq(
    // format: OFF
    "uk.gov.hmrc"    %% "bootstrap-frontend-play-30"            % bootstrapVersion,
    "uk.gov.hmrc"    %% "play-frontend-hmrc-play-30"            % "11.12.0",
    "org.typelevel"  %% "cats-core"                             % "2.13.0",
    "uk.gov.hmrc"    %% "play-conditional-form-mapping-play-30" % "3.2.0",
    "com.beachape"   %% "enumeratum-play"                       % "1.8.2",
    "io.lemonlabs"   %% "scala-uri"                             % "4.0.3"
  // format: ON
  )

  val test: Seq[ModuleID] = Seq(
    // format: OFF
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatestplus"            %% "mockito-3-2"            % "3.1.2.0",
    "org.wiremock"                  % "wiremock-standalone"    % "3.12.1",
    "org.jsoup"                     % "jsoup"                  % "1.19.1",
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "7.0.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.13.5"
  // format: ON
  ).map(_ % Test)
}
