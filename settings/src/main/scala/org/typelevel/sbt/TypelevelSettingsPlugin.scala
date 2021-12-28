package org.typelevel.sbt

import sbt._, Keys._
import sbt.plugins.JvmPlugin
import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.git
import org.typelevel.sbt.kernel.V

import scala.util.Try

object TypelevelSettingsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin && GitPlugin

  object autoImport {
    lazy val tlFatalWarnings =
      settingKey[Boolean]("Convert compiler warnings into errors (default: false)")
  }

  import autoImport._
  import TypelevelKernelPlugin.autoImport._

  override def globalSettings = Seq(
    tlFatalWarnings := false,
    Def.derive(scalaVersion := crossScalaVersions.value.last, default = true),
  )

  override def buildSettings = Seq(
    scmInfo := getScmInfo()
  )

  override def projectSettings = Seq(
    versionScheme := Some("early-semver"),

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
      if (tlFatalWarnings.value)
        Seq("-Xfatal-warnings")
      else
        Seq.empty
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
    },
    scalacOptions ++= {
      if (tlIsScala3.value && crossScalaVersions.value.forall(_.startsWith("3.")))
        Seq("-Ykind-projector:underscores")
      else if (tlIsScala3.value)
        Seq("-language:implicitConversions", "-Ykind-projector", "-source:3.0-migration")
      else
        Seq("-language:_")
    },
    Test / scalacOptions ++= {
      if (tlIsScala3.value)
        Seq()
      else
        Seq("-Yrangepos")
    },
    Compile / console / scalacOptions --= Seq(
      "-Xlint",
      "-Ywarn-unused-import",
      "-Wextra-implicit",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:imports",
      "-Wunused:locals",
      "-Wunused:params",
      "-Wunused:patvars",
      "-Wunused:privates"
    ),
    Test / console / scalacOptions := (Compile / console / scalacOptions).value,
    Compile / doc / scalacOptions ++= {
      if (tlIsScala3.value)
        Seq("-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath)
      else {

        val tagOrHash = getTagOrHash(git.gitCurrentTags.value, git.gitHeadCommit.value)

        val infoOpt = scmInfo.value
        tagOrHash.toSeq flatMap { vh =>
          infoOpt.toSeq flatMap { info =>
            val path = s"${info.browseUrl}/blob/$vh€{FILE_PATH}.scala"
            Seq(
              "-doc-source-url",
              path,
              "-sourcepath",
              (LocalRootProject / baseDirectory).value.getAbsolutePath)
          }
        }
      }
    },
    javacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-Xlint:all"
    ),
    javacOptions ++= {
      if (tlFatalWarnings.value)
        Seq("-Werror")
      else
        Seq.empty
    }
  )

  def getTagOrHash(tags: Seq[String], hash: Option[String]): Option[String] =
    tags.collect { case v @ V.Tag(_) => v }.headOption.orElse(hash)

  def getScmInfo(): Option[ScmInfo] = {
    import scala.sys.process._

    val identifier = """([^\/]+)"""

    val GitHubHttps = s"https://github.com/$identifier/$identifier".r
    val SSHConnection = s"git@github.com:$identifier/$identifier.git".r

    Try(List("git", "ls-remote", "--get-url", "origin").!!.trim())
      .collect {
        case GitHubHttps(user, repo) => (user, repo)
        case SSHConnection(user, repo) => (user, repo)
      }
      .map {
        case (user, repo) =>
          ScmInfo(url(s"https://github.com/$user/$repo"), s"git@github.com:$user/$repo.git")
      }
      .toOption
  }

}
