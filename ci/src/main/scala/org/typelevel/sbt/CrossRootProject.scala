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

import sbt._
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._

/**
 * Simultaneously creates a `root`, `rootJVM`, `rootJS`, and `rootNative` project, and
 * automatically enables the `NoPublishPlugin`.
 */
final class CrossRootProject private (
    val root: Project,
    val rootJVM: Project,
    val rootJS: Project,
    val rootNative: Project
) extends CompositeProject {

  override def componentProjects: Seq[Project] = Seq(root, rootJVM, rootJS, rootNative)

  def settings(ss: Def.SettingsDefinition*): CrossRootProject =
    new CrossRootProject(
      root.settings(ss: _*),
      rootJVM.settings(ss: _*),
      rootJS.settings(ss: _*),
      rootNative.settings(ss: _*)
    )

  def configure(transforms: (Project => Project)*): CrossRootProject =
    new CrossRootProject(
      root.configure(transforms: _*),
      rootJVM.configure(transforms: _*),
      rootJS.configure(transforms: _*),
      rootNative.configure(transforms: _*)
    )

  def enablePlugins(ns: Plugins*): CrossRootProject =
    new CrossRootProject(
      root.enablePlugins(ns: _*),
      rootJVM.enablePlugins(ns: _*),
      rootJS.enablePlugins(ns: _*),
      rootNative.enablePlugins(ns: _*)
    )

  def disablePlugins(ps: AutoPlugin*): CrossRootProject =
    new CrossRootProject(
      root.disablePlugins(ps: _*),
      rootJVM.disablePlugins(ps: _*),
      rootJS.disablePlugins(ps: _*),
      rootNative.disablePlugins(ps: _*)
    )

  def aggregate(projects: CompositeProject*): CrossRootProject =
    aggregateImpl(projects.flatMap(_.componentProjects): _*)

  private def aggregateImpl(projects: Project*): CrossRootProject = {
    val jsProjects =
      projects.filter(_.plugins.toString.contains("org.scalajs.sbtplugin.ScalaJSPlugin"))

    val nativeProjects =
      projects.filter(
        _.plugins.toString.contains("scala.scalanative.sbtplugin.ScalaNativePlugin"))

    val jvmProjects = projects.diff(jsProjects).diff(nativeProjects)

    new CrossRootProject(
      root.aggregate(projects.map(_.project): _*),
      if (jvmProjects.nonEmpty)
        rootJVM.aggregate(jvmProjects.map(_.project): _*).enablePlugins(TypelevelCiJVMPlugin)
      else rootJVM,
      if (jsProjects.nonEmpty)
        rootJS.aggregate(jsProjects.map(_.project): _*).enablePlugins(TypelevelCiJSPlugin)
      else rootJS,
      if (nativeProjects.nonEmpty)
        rootNative
          .aggregate(nativeProjects.map(_.project): _*)
          .enablePlugins(TypelevelCiNativePlugin)
      else rootNative
    )
  }

}

object CrossRootProject {
  private[sbt] def apply(): CrossRootProject = new CrossRootProject(
    Project("root", file(".")),
    Project("rootJVM", file(".jvm")),
    Project("rootJS", file(".js")),
    Project("rootNative", file(".native"))
  ).enablePlugins(NoPublishPlugin, TypelevelCiCrossPlugin)
}

/**
 * This plugin is used internally by CrossRootProject.
 */
object TypelevelCiCrossPlugin extends AutoPlugin {
  override def requires = TypelevelCiPlugin

  override def buildSettings = Seq(
    githubWorkflowBuildSbtStepPreamble ~= { s"project $${{ matrix.project }}" +: _ },
    githubWorkflowBuildMatrixAdditions += "project" -> Nil
  )
}

// The following plugins are used internally to support CrossRootProject.

object TypelevelCiJVMPlugin extends AutoPlugin {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    githubWorkflowBuildMatrixAdditions ~= { matrix =>
      matrix.updated("project", matrix("project") ::: "rootJVM" :: Nil)
    }
  )
}

object TypelevelCiJSPlugin extends AutoPlugin {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    githubWorkflowBuildMatrixAdditions ~= { matrix =>
      matrix.updated("project", matrix("project") ::: "rootJS" :: Nil)
    },
    githubWorkflowBuildMatrixExclusions ++= {
      githubWorkflowJavaVersions
        .value
        .tail
        .map(java => MatrixExclude(Map("project" -> "rootJS", "java" -> java.render)))
    },
    githubWorkflowBuild ~= { steps =>
      steps.flatMap {
        case testStep @ WorkflowStep.Sbt(List("test"), _, _, _, _, _) =>
          List(WorkflowStep.Sbt(List("Test/fastOptJS")), testStep)
        case step => List(step)
      }
    }
  )

}

object TypelevelCiNativePlugin extends AutoPlugin {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    githubWorkflowBuildMatrixAdditions ~= { matrix =>
      matrix.updated("project", matrix("project") ::: "rootNative" :: Nil)
    },
    githubWorkflowBuildMatrixExclusions ++= {
      githubWorkflowJavaVersions
        .value
        .tail
        .map(java => MatrixExclude(Map("project" -> "rootNative", "java" -> java.render)))
    },
    githubWorkflowBuild ~= { steps =>
      steps.flatMap {
        case testStep @ WorkflowStep.Sbt(List("test"), _, _, _, _, _) =>
          List(WorkflowStep.Sbt(List("Test/nativeLink")), testStep)
        case step => List(step)
      }
    }
  )
}
