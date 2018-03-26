package ch.epfl.bluebrain.nexus.workbench

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbt.complete.DefaultParsers._

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
    val targetExportSchemas = SettingKey[File](
      "target-export-schemas",
      "The default location where schemas are exported."
    )
    val targetCollectedResources = SettingKey[File](
      "target-collected-resources",
      "The default location where collected resources are stored."
    )
    val typeCollectedResources = SettingKey[String](
      "type-collected-resources",
      "The default collected resource types."
    )
    val exportSchemas = InputKey[Unit](
      "export-schemas",
      "Exports all schemas in the directory identified by 'target-export-schemas'; " +
        "the task accepts the 'baseUri' and 'exportTarget' location optional positional arguments."
    )
    val collectResources = InputKey[Unit](
      "collect-resources",
      "Collect all resources in the classpath with a type identified by 'type-collected-resources' and stored them in the directory identified by 'target-collected-resources';" +
        "the task accepts the 'resourceType' and 'collectTarget' location optional positional arguments."
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
    )(streams.value),
    sourceGenerators in Test += generateWorkbenchSpec.taskValue,
    targetExportSchemas := target.value / "exported",
    targetCollectedResources := target.value / "collected",
    typeCollectedResources := "all",
    exportSchemas := {
      Def.inputTaskDyn {
        val args = spaceDelimited("<arg>")
          .examples(
            "exportSchemas <baseUri> <export_target>",
            "exportSchemas http://localhost:8080/v0 /tmp/my-schemas"
          )
          .parsed
          .toList
        val completeArgs = args match {
          case Nil =>
            List(baseUri.value,
                 baseUriToken.value,
                 (resourceDirectory in Compile).value.getAbsolutePath,
                 targetExportSchemas.value.getAbsolutePath)
          case first :: Nil =>
            List(first,
                 baseUriToken.value,
                 (resourceDirectory in Compile).value.getAbsolutePath,
                 targetExportSchemas.value.getAbsolutePath)
          case first :: second :: _ =>
            List(first, baseUriToken.value, (resourceDirectory in Compile).value.getAbsolutePath, second)
        }
        val arguments = completeArgs.mkString(" ")
        streams.value.log.info(s"Running export-schemas with '$arguments'")
        runTask(Test, "ch.epfl.bluebrain.nexus.workbench.SchemaExport", completeArgs: _*)
      }.evaluated
    },
    collectResources := {
      Def.inputTaskDyn {
        val args = spaceDelimited("<arg>")
          .examples(
            "collectResources  <baseUri> <collect_target> <resource_type>",
            "collectResources http://localhost:8080/v0 ./target/collected schemas"
          )
          .parsed
          .toList
        val completeArgs = args match {
          case Nil =>
            List(baseUri.value,
                 baseUriToken.value,
                 targetCollectedResources.value.getAbsolutePath,
                 typeCollectedResources.value)
          case first :: Nil =>
            List(first,
                 baseUriToken.value,
                 targetCollectedResources.value.getAbsolutePath,
                 typeCollectedResources.value)
          case first :: second :: Nil =>
            List(first, baseUriToken.value, second, typeCollectedResources.value)
          case first :: second :: third :: _ =>
            List(first, baseUriToken.value, second, third)
        }
        val arguments = completeArgs.mkString(" ")
        streams.value.log.info(s"Running collect-resources with '$arguments'")
        runTask(Test, "ch.epfl.bluebrain.nexus.workbench.CollectResources", completeArgs: _*)
      }.evaluated
    }
  )

  private def generateSpec(target: File, baseUri: String, baseUriToken: String, baseDir: File, testDir: File)(
      streams: TaskStreams): Seq[File] = {
    val file = target / "ch" / "epfl" / "bluebrain" / "nexus" / "workbench" / "WorkbenchSpec.scala"
    if (file.exists()) {
      streams.log.info(s"Workbench spec '${file.getAbsolutePath}' already exists, skipping generation.")
      Seq(file)
    } else {
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
      streams.log.info(s"Generated workbench spec '${file.getAbsolutePath}'.")
      Seq(file)
    }
  }
}
