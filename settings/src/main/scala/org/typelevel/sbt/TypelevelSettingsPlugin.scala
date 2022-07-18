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

import io.github.davidgregory084.TpolecatPlugin
import org.typelevel.sbt.kernel.V
import sbt._
import sbtcrossproject.CrossPlugin.autoImport._
import sbtcrossproject.CrossType

import java.io.File
import java.lang.management.ManagementFactory
import scala.annotation.nowarn
import scala.util.Try

import Keys._

object TypelevelSettingsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = TypelevelKernelPlugin && TpolecatPlugin

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
      if (tlIsScala3.value)
        Nil
      else
        Seq(
          compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
        )
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
        Seq.empty
      else
        Seq("-Yrangepos")
    },
    Compile / doc / scalacOptions ++= {
      Seq("-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath)
    },
    Compile / doc / scalacOptions ++= {
      if (tlIsScala3.value)
        Seq("-project-version", version.value)
      else Nil
    },
    javacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-Xlint:all"
    ),

    // TODO make these respect Compile/Test config
    scalacOptions ++= {
      if (tlFatalWarnings.value)
        Seq("-Xfatal-warnings")
      else
        Seq.empty
    },
    javacOptions ++= {
      if (tlFatalWarnings.value)
        Seq("-Werror")
      else
        Seq.empty
    },
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

        case V(V(2, 13, _, _)) =>
          releaseOption ++ newTargetOption

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
    unmanagedSourceDirectories ++= {
      def extraDirs(suffix: String) =
        if (crossProjectPlatform.?.value.isDefined)
          List(CrossType.Pure, CrossType.Full).flatMap {
            _.sharedSrcDir(baseDirectory.value, Defaults.nameForSrc(configuration.value.name))
              .toList
              .map(f => file(f.getPath + suffix))
          }
        else
          List(
            baseDirectory.value / "src" / Defaults.nameForSrc(
              configuration.value.name) / s"scala$suffix"
          )

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, y)) if y <= 12 => extraDirs("-2.12-")
        case Some((2, y)) if y >= 13 => extraDirs("-2.13+")
        case Some((3, _)) => extraDirs("-2.13+")
        case _ => Nil
      }
    },
    packageSrc / mappings ++= {
      val base = sourceManaged.value
      managedSources.value.map(file => file -> file.relativeTo(base).get.getPath)
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
    val baseUrl = javaMajorVersion match {
      case v if v < 11 => url(s"https://docs.oracle.com/javase/${v}/docs/api/")
      case v => url(s"https://docs.oracle.com/en/java/javase/${v}/docs/api/java.base/")
    }
    doc / apiMappings ~= { old =>
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
