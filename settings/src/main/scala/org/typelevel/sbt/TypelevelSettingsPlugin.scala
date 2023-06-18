/*
 * Copyright 2022 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.sbt

import com.github.sbt.git.GitPlugin
import org.typelevel.sbt.kernel.V
import sbt._
import sbtcrossproject.CrossPlugin.autoImport._

import java.io.File
import java.lang.management.ManagementFactory
import scala.annotation.nowarn
import scala.util.Try

import Keys._

object TypelevelSettingsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = TypelevelKernelPlugin && GitPlugin

  object autoImport {
    lazy val tlFatalWarnings =
      settingKey[Boolean]("Convert compiler warnings into errors (default: false)")
    lazy val tlJdkRelease =
      settingKey[Option[Int]](
        "JVM target version for the compiled bytecode, None results in default scalac and javac behavior (no --release flag is specified). (default: None, supported values: 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)")
  }

  import autoImport._
  import TypelevelKernelPlugin.autoImport._

  override def globalSettings = Seq(
    tlFatalWarnings := false,
    tlJdkRelease := Some(8),
    Def.derive(scalaVersion := crossScalaVersions.value.last, default = true)
  )

  override def projectSettings = Seq(
    pomIncludeRepository := { _ => false },
    libraryDependencies ++= {
      val plugins =
        if (tlIsScala3.value)
          Nil
        else
          Seq(
            compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
            compilerPlugin(
              "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
          )

      Seq(
        "org.typelevel" %% "scalac-compat-annotation" % "0.1.0" % CompileTime
      ) ++ plugins
    },

    // Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8", // yes, this is 2 args
      "-feature",
      "-unchecked"),
    scalacOptions ++= {
      val warningsNsc = Seq(
        "-Xlint",
        "-Yno-adapted-args", // similar to '-Xlint:adapted-args' but fails compilation instead of just emitting a warning
        "-Ywarn-dead-code",
        "-Ywarn-unused-import"
      )

      val warnings211 = Seq(
        "-Ywarn-numeric-widen" // In 2.10 this produces a some strange spurious error
      )

      val removed212 = Set(
        "-Xlint",
        "-Yno-adapted-args", // mostly superseded by '-Xlint:adapted-args'
        "-Ywarn-unused-import" // superseded by '-Ywarn-unused:imports'
      )
      val warnings212 = Seq(
        // Tune '-Xlint':
        // - remove 'unused' because it is configured by '-Ywarn-unused'
        "-Xlint:_,-unused",
        // Tune '-Ywarn-unused':
        // - remove 'nowarn' because 2.13 can detect more unused cases than 2.12
        // - remove 'privates' because 2.12 can incorrectly detect some private objects as unused
        "-Ywarn-unused:_,-nowarn,-privates"
      )

      val removed213 = Set(
        "-Xlint:_,-unused", // reconfigured for 2.13
        "-Ywarn-unused:_,-nowarn,-privates", // mostly superseded by "-Wunused"
        "-Ywarn-dead-code", // superseded by "-Wdead-code"
        "-Ywarn-numeric-widen" // superseded by "-Wnumeric-widen"
      )
      val warnings213 = Seq(
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Wunused", // all choices are enabled by default
        "-Wvalue-discard",
        // Tune '-Xlint':
        // - remove 'implicit-recursion' due to backward incompatibility with 2.12
        // - remove 'recurse-with-default' due to backward incompatibility with 2.12
        // - remove 'unused' because it is configured by '-Wunused'
        "-Xlint:_,-implicit-recursion,-recurse-with-default,-unused"
      )

      val warningsDotty = Seq.empty

      val warnings33 = Seq(
        "-Wunused:implicits",
        "-Wunused:explicits",
        "-Wunused:imports",
        "-Wunused:locals",
        "-Wunused:params",
        "-Wunused:privates",
        "-Wvalue-discard"
      )

      scalaVersion.value match {
        case V(V(3, minor, _, _)) if minor >= 3 =>
          warnings33

        case V(V(3, _, _, _)) =>
          warningsDotty

        case V(V(2, minor, _, _)) if minor >= 13 =>
          (warnings211 ++ warnings212 ++ warnings213 ++ warningsNsc)
            .filterNot(removed212 ++ removed213)

        case V(V(2, minor, _, _)) if minor >= 12 =>
          (warnings211 ++ warnings212 ++ warningsNsc).filterNot(removed212)

        case V(V(2, minor, _, _)) if minor >= 11 =>
          warnings211 ++ warningsNsc

        case _ => Seq.empty
      }
    },
    scalacOptions ++= {
      scalaVersion.value match {
        case V(V(2, 12, _, _)) =>
          Seq("-Ypartial-unification")

        case V(V(2, 11, Some(build), _)) if build >= 11 =>
          Seq("-Ypartial-unification")

        case _ =>
          Seq.empty
      }
    },
    scalacOptions ++= {
      val numCPUs = java.lang.Runtime.getRuntime.availableProcessors()
      val settings = Seq(s"-Ybackend-parallelism", scala.math.min(16, numCPUs).toString)

      scalaVersion.value match {
        case V(V(2, 12, Some(build), _)) if build >= 5 =>
          settings

        case V(V(2, 13, _, _)) =>
          settings

        case _ =>
          Seq.empty
      }
    },
    scalacOptions ++= {
      scalaVersion.value match {
        case V(V(3, _, _, _)) if crossScalaVersions.value.forall(_.startsWith("3.")) =>
          Seq("-Ykind-projector:underscores")

        case V(V(3, _, _, _)) =>
          Seq("-language:implicitConversions", "-Ykind-projector")

        case V(V(2, minor, _, _)) if minor >= 12 =>
          Seq("-language:_", "-Xsource:3")

        case _ => Seq("-language:_")
      }
    },
    Test / scalacOptions ++= {
      if (tlIsScala3.value)
        Seq.empty
      else
        Seq("-Yrangepos")
    },
    Compile / console / scalacOptions := scalacOptions.value.filterNot { opt =>
      opt.startsWith("-Xlint") ||
      PartialFunction.cond(scalaVersion.value) {
        case V(V(2, minor, _, _)) if minor >= 13 =>
          opt.startsWith("-Wunused") || opt == "-Wextra-implicit"
        case V(V(2, minor, _, _)) if minor >= 12 =>
          opt.startsWith("-Ywarn-unused")
      }
    },
    Test / console / scalacOptions := (Compile / console / scalacOptions).value,
    Compile / doc / scalacOptions ++= {
      Seq("-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath)
    },
    Compile / doc / scalacOptions ++= {
      if (tlIsScala3.value)
        Seq("-project-version", version.value)
      else Nil
    },
    Compile / doc / scalacOptions ++= {
      // Enable Inkuire for Scala 3.2.1+
      scalaVersion.value match {
        case V(V(3, 2, Some(build), _)) if build >= 1 =>
          Seq("-Ygenerate-inkuire")
        case V(V(3, minor, _, _)) if minor >= 3 =>
          Seq("-Ygenerate-inkuire")
        case _ =>
          Seq.empty
      }
    },
    javacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-Xlint:all"
    ),
    scalacOptions ++= {
      val (releaseOption, newTargetOption, oldTargetOption) =
        withJdkRelease(tlJdkRelease.value)(
          (Seq.empty[String], Seq.empty[String], Seq.empty[String])) { n =>
          (Seq("-release", n.toString), Seq(s"-target:$n"), Seq("-target:jvm-1.8"))
        }

      scalaVersion.value match {
        case V(V(2, 11, _, _)) =>
          oldTargetOption

        case V(V(2, 12, Some(build), _)) if build >= 5 =>
          releaseOption ++ oldTargetOption

        case V(V(2, 13, Some(build), _)) if build <= 8 =>
          releaseOption ++ newTargetOption

        case V(V(2, 13, Some(build), _)) if build >= 9 =>
          releaseOption

        case V(V(3, _, _, _)) =>
          releaseOption

        case _ =>
          Seq.empty
      }
    },
    javacOptions ++= {
      withJdkRelease(tlJdkRelease.value)(Seq.empty[String])(n => Seq("--release", n.toString))
    },
    javaApiMappings
  ) ++ inConfig(Compile)(perConfigSettings) ++ inConfig(Test)(perConfigSettings)

  private val perConfigSettings = Seq(
    scalacOptions := {
      val old = scalacOptions.value
      val flag = "-Werror"
      if (tlFatalWarnings.value)
        if (!old.contains(flag)) old :+ flag else old
      else
        old.filterNot(_ == flag)
    },
    javacOptions ++= {
      val old = javacOptions.value
      val flag = "-Werror"
      if (tlFatalWarnings.value)
        if (!old.contains(flag)) old :+ flag else old
      else
        old.filterNot(_ == flag)
    },
    unmanagedSourceDirectories ++= {
      def extraDirs(suffix: String) =
        crossProjectCrossType.?.value match {
          case Some(crossType) =>
            crossType
              .sharedSrcDir(baseDirectory.value, Defaults.nameForSrc(configuration.value.name))
              .toList
              .map(f => file(f.getPath + suffix))
          case None =>
            List(
              baseDirectory.value / "src" /
                Defaults.nameForSrc(configuration.value.name) / s"scala$suffix"
            )
        }

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, y)) if y <= 12 => extraDirs("-2.12-")
        case Some((2, y)) if y >= 13 => extraDirs("-2.13+")
        case Some((3, _)) => extraDirs("-2.13+")
        case _ => Nil
      }
    },
    packageSrc / mappings ++= {
      val base = sourceManaged.value
      managedSources.value.map { file =>
        file.relativeTo(base) match {
          case Some(relative) => file -> relative.getPath
          case None =>
            throw new RuntimeException(
              s"""|Expected managed sources in:
                  |$base
                  |But found them here:
                  |$file
                  |""".stripMargin
            )
        }
      }
    }
  )

  private def withJdkRelease[A](jdkRelease: Option[Int])(default: => A)(f: Int => A): A =
    jdkRelease.fold(default) {
      case 8 if isJava8 => default
      case n if n >= 8 =>
        if (javaMajorVersion < n) {
          sys.error(
            s"Target JDK is $n but you are using an older JDK $javaMajorVersion. Please switch to JDK >= $n.")
        } else {
          f(n)
        }
      case n =>
        sys.error(
          s"Target JDK is $n, which is not supported by `sbt-typelevel`. Please select a JDK >= 8.")
    }

  private val javaMajorVersion: Int =
    System.getProperty("java.version").stripPrefix("1.").takeWhile(_.isDigit).toInt

  private val isJava8: Boolean = javaMajorVersion == 8

  private val javaApiMappings = {
    // scaladoc doesn't support this automatically before 2.13
    doc / apiMappings := {
      val old = (doc / apiMappings).value

      val baseUrl = tlJdkRelease.value.getOrElse(javaMajorVersion) match {
        case v if v < 11 => url(s"https://docs.oracle.com/javase/${v}/docs/api/")
        case v => url(s"https://docs.oracle.com/en/java/javase/${v}/docs/api/java.base/")
      }

      val runtimeMXBean = ManagementFactory.getRuntimeMXBean
      val oldSchool = Try(
        if (runtimeMXBean.isBootClassPathSupported)
          runtimeMXBean
            .getBootClassPath
            .split(File.pathSeparatorChar)
            .map(file(_) -> baseUrl)
            .toMap
        else Map.empty
      ).getOrElse(Map.empty)
      val newSchool = Map(file("/modules/java.base") -> baseUrl)
      // Latest one wins.  We are providing a fallback.
      oldSchool ++ newSchool ++ old
    }
  }

  @nowarn("cat=unused")
  private[this] def unused(): Unit = ()
}
