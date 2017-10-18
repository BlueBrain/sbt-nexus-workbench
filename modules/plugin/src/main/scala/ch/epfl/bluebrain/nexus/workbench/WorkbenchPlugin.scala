package ch.epfl.bluebrain.nexus.workbench

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object WorkbenchPlugin extends AutoPlugin {

  override lazy val requires = JvmPlugin
  override lazy val trigger  = noTrigger

  trait Keys {
    val baseUri = SettingKey[String](
      "base-uri",
      "The base uri to be used when expanding addresses in resources."
    )
    val baseUriToken = SettingKey[String](
      "base-uri-token",
      "The token to replace when loading resources."
    )
    val workbenchVersion = SettingKey[String](
      "workbench-version",
      "The version of the workbench to be added as a library dependency."
    )
    val generateWorkbenchSpec = TaskKey[Seq[File]](
      "generate-workbench-spec",
      "Generates a ScalaTest spec that discovers and tests schemas."
    )
  }
  object autoImport extends Keys
  import autoImport._

  override lazy val projectSettings = Seq(
    libraryDependencies += "ch.epfl.bluebrain.nexus" %% "nexus-workbench" % workbenchVersion.value % Test,
    baseUri := "http://localhost/v0",
    baseUriToken := "{{base}}",
    generateWorkbenchSpec := generateSpec(
      (sourceManaged in Test).value,
      baseUri.value,
      baseUriToken.value,
      (resourceDirectory in Compile).value,
      (resourceDirectory in Test).value
    ),
    sourceGenerators in Test += generateWorkbenchSpec.taskValue
  )

  private def generateSpec(target: File,
                           baseUri: String,
                           baseUriToken: String,
                           baseDir: File,
                           testDir: File): Seq[File] = {
    val file = target / "ch" / "epfl" / "bluebrain" / "nexus" / "workbench" / "WorkbenchSpec.scala"
    if (file.exists()) Seq(file)
    else {
      val content =
        s"""|package ch.epfl.bluebrain.nexus.workbench
            |
            |class WorkbenchSpec extends WorkbenchSpecLike {
            |  override def baseUri: String = "$baseUri"
            |  override def baseUriToken: String = "$baseUriToken"
            |  override def baseDir: String = "${baseDir.getAbsolutePath}"
            |  override def testDir: String = "${testDir.getAbsolutePath}"
            |}""".stripMargin
      IO.write(file, content)
      Seq(file)
    }
  }
}
