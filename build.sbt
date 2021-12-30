name := "sbt-typelevel"

ThisBuild / tlBaseVersion := "0.4"
ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"

ThisBuild / scalaVersion := "2.12.15"

enablePlugins(TypelevelCiReleasePlugin)
ThisBuild / tlCiReleaseSnapshots := true
ThisBuild / tlCiReleaseBranches := Seq("series/armanbilge")
ThisBuild / tlSonatypeUseLegacyHost := true

ThisBuild / developers := List(
  Developer("armanbilge", "Arman Bilge", "@armanbilge", url("https://github.com/armanbilge")),
  Developer("rossabaker", "Ross A. Baker", "@rossabaker", url("https://github.com/rossabaker")),
  Developer(
    "ChristopherDavenport",
    "Christopher Davenport",
    "@ChristopherDavenport",
    url("https://github.com/ChristopherDavenport")),
  Developer("djspiewak", "Daniel Spiewak", "@djspiewak", url("https://github.com/djspiewak"))
)
ThisBuild / homepage := Some(url("https://github.com/typelevel/sbt-typelevel"))
ThisBuild / licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/")

lazy val root = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .aggregate(
    kernel,
    noPublish,
    settings,
    versioning,
    mima,
    sonatype,
    ciSigning,
    sonatypeCiRelease,
    ci,
    core,
    ciRelease)

lazy val kernel = project
  .in(file("kernel"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-kernel"
  )

lazy val noPublish = project
  .in(file("no-publish"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-no-publish"
  )

lazy val settings = project
  .in(file("settings"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-settings"
  )
  .dependsOn(kernel)

lazy val versioning = project
  .in(file("versioning"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-versioning"
  )
  .dependsOn(kernel)

lazy val mima = project
  .in(file("mima"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-mima"
  )
  .dependsOn(kernel)

lazy val sonatype = project
  .in(file("sonatype"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-sonatype"
  )

lazy val ciSigning = project
  .in(file("ci-signing"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci-signing"
  )

lazy val sonatypeCiRelease = project
  .in(file("sonatype-ci-release"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-sonatype-ci-release"
  )
  .dependsOn(sonatype)

lazy val ci = project
  .in(file("ci"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci"
  )

lazy val core = project
  .in(file("core"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel"
  )
  .dependsOn(
    noPublish,
    settings,
    versioning,
    mima,
    ci
  )

lazy val ciRelease = project
  .in(file("ci-release"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci-release"
  )
  .dependsOn(
    core,
    sonatype,
    ciSigning,
    sonatypeCiRelease
  )
