name := "sbt-typelevel"

ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = project.in(file("."))

lazy val noPublish = project
  .in(file("no-publish"))
  .enablePlugins(SbtPlugin, NoPublishPlugin)
  .settings(
    name := "sbt-typlevel-no-publish"
  )
