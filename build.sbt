/*
scalafmt: {
  style = defaultWithAlign
  maxColumn = 150
  align.tokens = [
    { code = "=>", owner = "Case" }
    { code = "?", owner = "Case" }
    { code = "extends", owner = "Defn.(Class|Trait|Object)" }
    { code = "//", owner = ".*" }
    { code = "{", owner = "Template" }
    { code = "}", owner = "Template" }
    { code = ":=", owner = "Term.ApplyInfix" }
    { code = "++=", owner = "Term.ApplyInfix" }
    { code = "+=", owner = "Term.ApplyInfix" }
    { code = "%", owner = "Term.ApplyInfix" }
    { code = "%%", owner = "Term.ApplyInfix" }
    { code = "%%%", owner = "Term.ApplyInfix" }
    { code = "->", owner = "Term.ApplyInfix" }
    { code = "?", owner = "Term.ApplyInfix" }
    { code = "<-", owner = "Enumerator.Generator" }
    { code = "?", owner = "Enumerator.Generator" }
    { code = "=", owner = "(Enumerator.Val|Defn.(Va(l|r)|Def|Type))" }
  ]
}
 */
val commonsVersion   = "0.10.8"
val akkaHttpVersion  = "10.0.11"
val scalaTestVersion = "3.0.5"
val sbtIoVersion     = "1.1.4"

lazy val shaclValidator = "ch.epfl.bluebrain.nexus" %% "shacl-validator" % commonsVersion
lazy val akkaHttpCore   = "com.typesafe.akka"       %% "akka-http-core"  % akkaHttpVersion
lazy val scalaTest      = "org.scalatest"           %% "scalatest"       % scalaTestVersion
lazy val sbtIo          = "org.scala-sbt"           %% "io"              % sbtIoVersion

lazy val workbench = project
  .in(file("modules/workbench"))
  .settings(
    name       := "nexus-workbench",
    moduleName := "nexus-workbench",
    libraryDependencies ++= Seq(
      akkaHttpCore,
      shaclValidator,
      sbtIo,
      scalaTest
    ),
    coverageFailOnMinimum := false
  )

lazy val plugin = project
  .in(file("modules/plugin"))
  .settings(
    name                  := "sbt-nexus-workbench",
    moduleName            := "sbt-nexus-workbench",
    sbtPlugin             := true,
    coverageFailOnMinimum := false
  )

lazy val root = project
  .in(file("."))
  .settings(
    noPublish,
    name                  := "workbench",
    moduleName            := "workbench",
    description           := "Nexus Workbench",
    coverageFailOnMinimum := false
  )
  .aggregate(workbench, plugin)

/* Common settings */

lazy val noPublish = Seq(
  publishLocal    := {},
  publish         := {},
  publishArtifact := false
)

inThisBuild(
  Seq(
    resolvers += Resolver.bintrayRepo("bogdanromanx", "maven"),
    homepage  := Some(new URL("https://github.com/BlueBrain/sbt-nexus-workbench")),
    licenses  := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
    scmInfo   := Some(ScmInfo(url("https://github.com/BlueBrain/sbt-nexus-workbench"), "scm:git:git@github.com:BlueBrain/sbt-nexus-workbench.git")),
    developers := List(
      Developer("bogdanromanx", "Bogdan Roman", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("hygt", "Henry Genet", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("umbreak", "Didac Montero Mendez", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("wwajerowicz", "Wojtek Wajerowicz", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/"))
    ),
    // These are the sbt-release-early settings to configure
    releaseEarlyWith              := BintrayPublisher,
    releaseEarlyNoGpg             := true,
    releaseEarlyEnableSyncToMaven := false
  )
)

addCommandAlias("review", ";clean;coverage;scapegoat;test;coverageReport;coverageAggregate")
