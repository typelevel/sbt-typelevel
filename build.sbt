name := "sbt-typelevel"

ThisBuild / tlBaseVersion := "0.4"
ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = project.in(file(".")).enablePlugins(NoPublishPlugin)

lazy val noPublish = project
  .in(file("no-publish"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-no-publish"
  )

lazy val versioning = project
  .in(file("versioning"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-versioning"
  )
