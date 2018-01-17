val commonsVersion   = "0.5.30"
val akkaHttpVersion  = "10.0.10"
val scalaTestVersion = "3.0.4"
val sbtIoVersion     = "1.1.0"

lazy val shaclValidator = "ch.epfl.bluebrain.nexus" %% "shacl-validator" % commonsVersion
lazy val akkaHttpCore   = "com.typesafe.akka"       %% "akka-http-core"  % akkaHttpVersion
lazy val scalaTest      = "org.scalatest"           %% "scalatest"       % scalaTestVersion
lazy val sbtIo          = "org.scala-sbt"           %% "io"              % sbtIoVersion

lazy val workbench = project
  .in(file("modules/workbench"))
  .settings(
    common,
    name := "nexus-workbench",
    moduleName := "nexus-workbench",
    libraryDependencies ++= Seq(
      shaclValidator,
      akkaHttpCore,
      sbtIo,
      scalaTest
    )
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
  coverageFailOnMinimum := false,
  homepage := Some(new URL("https://github.com/BlueBrain/sbt-nexus-workbench")),
  licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
  scmInfo := Some(
    ScmInfo(url("https://github.com/BlueBrain/sbt-nexus-workbench"),
            "scm:git:git@github.com:BlueBrain/sbt-nexus-workbench.git"))
)

addCommandAlias("review", ";clean;coverage;scapegoat;test;coverageReport;coverageAggregate")
addCommandAlias("rel", ";release with-defaults skip-tests")
