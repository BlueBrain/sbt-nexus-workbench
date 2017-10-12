val commonsVersion   = "0.5.0"
val scalaTestVersion = "3.0.4"

lazy val shaclValidator = "ch.epfl.bluebrain.nexus" %% "shacl-validator" % commonsVersion
lazy val scalaTest      = "org.scalatest"           %% "scalatest"       % scalaTestVersion

lazy val workbench = project
  .in(file("modules/workbench"))
  .settings(
    common,
    name := "nexus-workbench",
    moduleName := "nexus-workbench",
    libraryDependencies ++= Seq(shaclValidator, scalaTest % Test)
  )

lazy val plugin = project
  .in(file("modules/plugin"))
  .settings(
    common,
    name := "sbt-nexus-workbench",
    moduleName := "sbt-nexus-workbench",
    sbtPlugin := true
  )

lazy val root = project
  .in(file("."))
  .settings(
    common,
    noPublish,
    name := "workbench",
    moduleName := "workbench",
    description := "Nexus Workbench"
  )
  .aggregate(workbench, plugin)

/* Common settings */

lazy val noPublish = Seq(
  publishLocal := {},
  publish := {}
)

lazy val common = Seq(
  homepage := Some(new URL("https://github.com/BlueBrain/sbt-nexus-workbench")),
  licenses := Seq(("Apache 2.0", new URL("https://github.com/BlueBrain/sbt-nexus-workbench/blob/master/LICENSE")))
)

addCommandAlias("review", ";clean;coverage;scapegoat;test;coverageReport;coverageAggregate")
addCommandAlias("rel", ";release with-defaults skip-tests")
