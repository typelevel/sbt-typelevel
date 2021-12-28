name := "sbt-typelevel"

ThisBuild / tlBaseVersion := "0.4"
ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .aggregate(kernel, noPublish, settings, versioning, mima)

lazy val kernel = project
  .in(file("kernel"))
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

lazy val ciRelease = project
  .in(file("ci-release"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci-release"
  )
  .dependsOn(sonatype)

lazy val ci = project
  .in(file("ci"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci"
  )
