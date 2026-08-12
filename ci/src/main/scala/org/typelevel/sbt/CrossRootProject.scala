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

import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import sbt._
import sbt.internal.ProjectMatrix

import Keys._

/**
 * Simultaneously creates a root project, a Scala JVM aggregate project, a Scala.js aggregate
 * project, a Scala Native aggregate project and automatically enables the `NoPublishPlugin`.
 */
final class CrossRootProject private (
    val all: Project,
    val jvm: Map[String, Project],
    val js: Map[String, Project],
    val native: Map[String, Project]
) extends CompositeProject {

  override def componentProjects: Seq[Project] =
    Seq(all) ++
      jvm.values ++
      js.values ++
      native.values

  def settings(ss: Def.SettingsDefinition*): CrossRootProject =
    new CrossRootProject(
      all.settings(ss: _*),
      mapProject(jvm)(_.settings(ss: _*)),
      mapProject(js)(_.settings(ss: _*)),
      mapProject(native)(_.settings(ss: _*))
    )

  def configure(transforms: (Project => Project)*): CrossRootProject =
    new CrossRootProject(
      all.configure(transforms: _*),
      mapProject(jvm)(_.configure(transforms: _*)),
      mapProject(js)(_.configure(transforms: _*)),
      mapProject(native)(_.configure(transforms: _*))
    )

  // TODO: Zainab - See Laika and feral builds to understand how "all" is used.
  def configureRoot(transforms: (Project => Project)*): CrossRootProject =
    new CrossRootProject(
      all.configure(transforms: _*),
      jvm,
      js,
      native
    )

  def configureJVM(transforms: (Project => Project)*): CrossRootProject =
    new CrossRootProject(
      all,
      mapProject(jvm)(_.configure(transforms: _*)),
      js,
      native
    )

  def configureJS(transforms: (Project => Project)*): CrossRootProject =
    new CrossRootProject(
      all,
      jvm,
      mapProject(js)(_.configure(transforms: _*)),
      native
    )

  def configureNative(transforms: (Project => Project)*): CrossRootProject =
    new CrossRootProject(
      all,
      jvm,
      js,
      mapProject(native)(_.configure(transforms: _*))
    )

  def enablePlugins(ns: Plugins*): CrossRootProject =
    new CrossRootProject(
      all.enablePlugins(ns: _*),
      mapProject(jvm)(_.enablePlugins(ns: _*)),
      mapProject(js)(_.enablePlugins(ns: _*)),
      mapProject(native)(_.enablePlugins(ns: _*))
    )

  def disablePlugins(ps: AutoPlugin*): CrossRootProject =
    new CrossRootProject(
      all.disablePlugins(ps: _*),
      mapProject(jvm)(_.disablePlugins(ps: _*)),
      mapProject(js)(_.disablePlugins(ps: _*)),
      mapProject(native)(_.disablePlugins(ps: _*))
    )

  def aggregate(projects: ProjectMatrix*): CrossRootProject = {
    aggregateImpl(projects)
  }

  def aggregate(scalaVersion: String, projects: Project*): CrossRootProject = {
    val componentProjects = projects.flatMap(_.componentProjects)
    val jsProjects =
      componentProjects.filter(
        _.plugins.toString.contains("org.scalajs.sbtplugin.ScalaJSPlugin"))
    val nativeProjects =
      componentProjects.filter(
        _.plugins.toString.contains("scala.scalanative.sbtplugin.ScalaNativePlugin"))
    val jvmProjects =
      projects.diff(jsProjects).diff(nativeProjects)
    aggregateForScalaVersion(
      scalaVersion,
      jvmProjects,
      jsProjects,
      nativeProjects
    )
  }

  private def mapProject(kvs: Map[String, Project])(
      f: Project => Project): Map[String, Project] =
    kvs.map { case (k, v) => k -> f(v) }

  private def aggregateImpl(matrices: Seq[ProjectMatrix]): CrossRootProject = {

    def projectsForScalaVersion(scalaVersion: String, axis: VirtualAxis): Seq[Project] = {
      matrices.flatMap { matrix =>
        matrix.allProjects().collect {
          case (project, axes)
              if axes.contains(axis) &&
                axes.exists {
                  case VirtualAxis.ScalaVersionAxis(_, scalaBinaryVersion) =>
                    scalaBinaryVersion == scalaVersion
                  case _ => false
                } =>
            project
        }
      }
    }
    val scalaVersions = jvm.keys.toList
    scalaVersions.foldLeft(this) { (crossRootProj, scalaVersion) =>
      crossRootProj.aggregateForScalaVersion(
        scalaVersion,
        jvmProjects = projectsForScalaVersion(scalaVersion, VirtualAxis.jvm),
        jsProjects = projectsForScalaVersion(scalaVersion, VirtualAxis.js),
        nativeProjects = projectsForScalaVersion(scalaVersion, VirtualAxis.native)
      )
    }
  }

  private def aggregateForScalaVersion(
      scalaVersion: String,
      jvmProjects: Seq[Project],
      jsProjects: Seq[Project],
      nativeProjects: Seq[Project]): CrossRootProject = {
    val allProjects = jvmProjects ++ jsProjects ++ nativeProjects
    new CrossRootProject(
      all.aggregate(allProjects.map(_.project): _*),
      jvm = addProjects(jvm, scalaVersion, jvmProjects, TypelevelCiJVMPlugin),
      js = addProjects(js, scalaVersion, jsProjects, TypelevelCiJSPlugin),
      native = addProjects(native, scalaVersion, nativeProjects, TypelevelCiNativePlugin)
    )
  }

  private def addProjects(
      existingProjects: Map[String, Project],
      scalaVersion: String,
      additionalProjects: Seq[Project],
      plugin: AutoPlugin): Map[String, Project] = {
    if (additionalProjects.nonEmpty) {
      existingProjects + (scalaVersion -> existingProjects(scalaVersion)
        .aggregate(additionalProjects.map(_.project): _*)
        .enablePlugins(plugin))
    } else {
      existingProjects
    }
  }
}

object CrossRootProject {

  private val defaultScalaVersions: Seq[String] = Seq("2.12", "2.13", "3")

  // TODO: Zainab - Add a field for scala versions into the macro, or extract it from crossScalaVersions if possible.
  def apply(id: String): CrossRootProject =
    apply(id, defaultScalaVersions)

  def apply(id: String, scalaVersions: Seq[String]): CrossRootProject = new CrossRootProject(
    Project(id, file("."))
      .settings(crossScalaVersions := Nil, scalaVersion := (ThisBuild / scalaVersion).value),
    platformProject(id, scalaVersions, "JVM"),
    platformProject(id, scalaVersions, "JS"),
    platformProject(id, scalaVersions, "Native")
  ).enablePlugins(NoPublishPlugin, TypelevelCiCrossPlugin)

  private def platformProject(
      id: String,
      scalaVersions: Seq[String],
      platform: String): Map[String, Project] = {
    scalaVersions.map { version =>
      val suffix = version.replace(".", "_")
      version -> Project(s"${id}${platform}$suffix", file(s".${platform.toLowerCase}$suffix"))
    }.toMap
  }
}

/**
 * This trait provides an anonymous setting giving access to the local root project ID.
 */
private[sbt] trait RootProjectId {
  protected def rootProjectId = Def.setting {
    (LocalRootProject / Keys.thisProject).value.id
  }
}

/**
 * This plugin is used internally by CrossRootProject.
 */
object TypelevelCiCrossPlugin extends AutoPlugin {
  override def requires = TypelevelCiPlugin

  override def buildSettings = Seq(
    githubWorkflowBuildSbtStepPreamble ~= {
      s"project $${{ matrix.project }}$${{ matrix.scala }}" +: _
    },
    githubWorkflowBuildMatrixAdditions += "project" -> Nil,
    githubWorkflowArtifactDownloadExtraKeys += "project"
  )
}

// The following plugins are used internally to support CrossRootProject.

object TypelevelCiJVMPlugin extends AutoPlugin with RootProjectId {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    githubWorkflowBuildMatrixAdditions := {
      val matrix = githubWorkflowBuildMatrixAdditions.value
      matrix.updated("project", matrix("project") ::: s"${rootProjectId.value}JVM" :: Nil)
    }
  )
}

object TypelevelCiJSPlugin extends AutoPlugin with RootProjectId {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    githubWorkflowBuildMatrixAdditions := {
      val matrix = githubWorkflowBuildMatrixAdditions.value
      matrix.updated("project", matrix("project") ::: s"${rootProjectId.value}JS" :: Nil)
    },
    githubWorkflowBuildMatrixExclusions ++= {
      githubWorkflowJavaVersions
        .value
        .tail
        .map(java =>
          MatrixExclude(Map("project" -> s"${rootProjectId.value}JS", "java" -> java.render)))
    },
    githubWorkflowBuild := {
      githubWorkflowBuild.value.flatMap {
        case testStep: WorkflowStep.Sbt if testStep.commands == List("test") =>
          val fastOptStep = WorkflowStep.Sbt(
            List("Test/scalaJSLinkerResult"),
            name = Some("scalaJSLink"),
            cond = Some(s"matrix.project == '${rootProjectId.value}JS'")
          )
          List(fastOptStep, testStep)
        case step => List(step)
      }
    }
  )

}

object TypelevelCiNativePlugin extends AutoPlugin with RootProjectId {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    githubWorkflowBuildMatrixAdditions := {
      val matrix = githubWorkflowBuildMatrixAdditions.value
      matrix.updated("project", matrix("project") ::: s"${rootProjectId.value}Native" :: Nil)
    },
    githubWorkflowBuildMatrixExclusions ++= {
      githubWorkflowJavaVersions
        .value
        .tail
        .map(java =>
          MatrixExclude(
            Map("project" -> s"${rootProjectId.value}Native", "java" -> java.render)))
    },
    githubWorkflowBuild := {
      githubWorkflowBuild.value.flatMap {
        case testStep: WorkflowStep.Sbt if testStep.commands == List("test") =>
          val nativeLinkStep = WorkflowStep.Sbt(
            List("Test/nativeLink"),
            name = Some("nativeLink"),
            cond = Some(s"matrix.project == '${rootProjectId.value}Native'")
          )
          List(nativeLinkStep, testStep)
        case step => List(step)
      }
    }
  )
}
