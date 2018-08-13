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
val akkaHttpVersion  = "10.1.3"
val catsVersion      = "1.1.0"
val circeVersion     = "0.9.3"
val commonsVersion   = "0.10.17"
val journalVersion   = "3.0.19"
val rdfVersion       = "0.2.16"
val scalaTestVersion = "3.0.5"
val sbtIoVersion     = "1.1.10"
val springVersion    = "5.0.7.RELEASE"

lazy val akkaHttpCore     = "com.typesafe.akka"       %% "akka-http-core"              % akkaHttpVersion
lazy val catsCore         = "org.typelevel"           %% "cats-core"                   % catsVersion
lazy val circeCore        = "io.circe"                %% "circe-core"                  % circeVersion
lazy val commonsHttp      = "ch.epfl.bluebrain.nexus" %% "commons-http"                % commonsVersion
lazy val commonsTests     = "ch.epfl.bluebrain.nexus" %% "commons-test"                % commonsVersion
lazy val topQuadrantShacl = "ch.epfl.bluebrain.nexus" %% "shacl-topquadrant-validator" % commonsVersion
lazy val journal          = "io.verizon.journal"      %% "core"                        % journalVersion
lazy val rdfCirce         = "ch.epfl.bluebrain.nexus" %% "rdf-circe"                   % rdfVersion
lazy val scalaTest        = "org.scalatest"           %% "scalatest"                   % scalaTestVersion
lazy val sbtIo            = "org.scala-sbt"           %% "io"                          % sbtIoVersion
lazy val spring           = "org.springframework"     % "spring-core"                  % springVersion

lazy val workbench = project
  .in(file("modules/workbench"))
  .settings(
    name       := "nexus-workbench",
    moduleName := "nexus-workbench",
    resolvers  += Resolver.bintrayRepo("spring", "jars"),
    libraryDependencies ++= Seq(
      akkaHttpCore,
      catsCore,
      circeCore,
      commonsHttp,
      commonsTests,
      journal,
      rdfCirce,
      sbtIo,
      scalaTest,
      spring,
      topQuadrantShacl
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
