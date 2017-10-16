package ch.epfl.bluebrain.nexus.workbench

import sbt.Keys._
import sbt._

object WorkbenchPlugin extends AutoPlugin {

  override lazy val requires = empty
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
  }
  object autoImport extends Keys
  import autoImport._

  override lazy val projectSettings = Seq(
    baseUri := "http://localhost/v0",
    baseUriToken := "{{base}}",
    libraryDependencies += organization.value %% "nexus-workbench" % version.in(ThisBuild).value % Test,
    sourceGenerators in Test += {
      Def.task {
        val targetDir = (sourceManaged in Test).value
        val file      = targetDir / "ch" / "epfl" / "bluebrain" / "nexus" / "workbench" / "WorkbenchSpec.scala"
        val content =
          s"""
             |package ch.epfl.bluebrain.nexus.workbench
             |
             |import ch.epfl.bluebrain.nexus.workbench.WorkbenchSpecLike
             |
             |class WorkbenchSpec extends WorkbenchSpecLike {
             |  override def baseUri: String = "${baseUri.value}"
             |  override def baseUriToken: String = "${baseUriToken.value}"
             |  override def baseDir: String = "${(resourceDirectory in Compile).value}"
             |}
           """.stripMargin
        IO.write(file, content)
        Seq(file)
      }.taskValue
    }
  )
}
