sbtPlugin := true

name := "sbt-typelevel"

organization := "org.typelevel"

version := "0.1-SNAPSHOT"

// This is both a plugin and a meta-plugin

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.3")
