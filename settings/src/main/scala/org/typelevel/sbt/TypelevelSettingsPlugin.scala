package org.typelevel.sbt

import sbt._, Keys._
import sbt.plugins.JvmPlugin
import org.typelevel.sbt.kernel.V

object TypelevelSettingsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    lazy val tlIsScala3 = settingKey[Boolean]("True if building with Scala 3")
    lazy val tlFatalWarningsInCI = settingKey[Boolean](
      "Convert compiler warnings into errors under CI builds (default: true)")
  }

  import autoImport._

  override def globalSettings = Seq(
    tlFatalWarningsInCI := true,
    Def.derive(scalaVersion := crossScalaVersions.value.last, default = true),
    Def.derive(tlIsScala3 := scalaVersion.value.startsWith("3."))
  )

  override def projectSettings = Seq(
    libraryDependencies ++= {
      if (tlIsScala3.value)
        Nil
      else
        Seq(
          compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
        )
    },

    // Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8", // yes, this is 2 args
      "-feature",
      "-unchecked"),
    scalacOptions ++= {
      scalaVersion.value match {
        case V(V(2, minor, _, _)) if minor < 13 =>
          Seq("-Yno-adapted-args", "-Ywarn-unused-import")
        case _ =>
          Seq.empty
      }
    },
    scalacOptions ++= {
      val warningsNsc = Seq("-Xlint", "-Ywarn-dead-code")

      val warnings211 =
        Seq("-Ywarn-numeric-widen") // In 2.10 this produces a some strange spurious error

      val warnings212 = Seq("-Xlint:-unused,_")

      val removed213 = Set("-Xlint:-unused,_", "-Xlint")
      val warnings213 = Seq(
        "-Xlint:deprecation",
        "-Wunused:nowarn",
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Wunused:implicits",
        "-Wunused:explicits",
        "-Wunused:imports",
        "-Wunused:locals",
        "-Wunused:params",
        "-Wunused:patvars",
        "-Wunused:privates",
        "-Wvalue-discard"
      )

      val warningsDotty = Seq()

      scalaVersion.value match {
        case V(V(3, _, _, _)) =>
          warningsDotty

        case V(V(2, minor, _, _)) if minor >= 13 =>
          (warnings211 ++ warnings212 ++ warnings213 ++ warningsNsc).filterNot(removed213)

        case V(V(2, minor, _, _)) if minor >= 12 =>
          warnings211 ++ warnings212 ++ warningsNsc

        case V(V(2, minor, _, _)) if minor >= 11 =>
          warnings211 ++ warningsNsc

        case _ => Seq.empty
      }
    }
  )
}
