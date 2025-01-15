import com.timushev.sbt.updates.UpdatesKeys.dependencyUpdates
import com.timushev.sbt.updates.UpdatesPlugin.autoImport.{dependencyUpdatesFailBuild, dependencyUpdatesFilter, moduleFilterRemoveValue}
import sbt.*
import sbt.Keys.*

object SbtUpdatesSettings {

  lazy val sbtUpdatesSettings = Seq(
    dependencyUpdatesFailBuild := StrictBuilding.strictBuilding.value,
    (Compile / compile) := ((Compile / compile) dependsOn dependencyUpdates).value,
    dependencyUpdatesFilter -= moduleFilter("org.scala-lang"),
    dependencyUpdatesFilter -= moduleFilter("org.playframework"),
    // newer versions are needed of the jackson libraries below than those provided by play
    dependencyUpdatesFilter -= moduleFilter("com.fasterxml.jackson.core"),
    dependencyUpdatesFilter -= moduleFilter("com.fasterxml.jackson.datatype"),
    dependencyUpdatesFilter -= moduleFilter("com.fasterxml.jackson.dataformat"),
    dependencyUpdatesFilter -= moduleFilter("com.fasterxml.jackson.module"),
    // locked by version of play
    dependencyUpdatesFilter -= moduleFilter("org.scalatestplus.play", "scalatestplus-play")
  )

}
