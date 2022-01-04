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
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbtghactions.GenerativePlugin.autoImport._
import scala.scalanative.sbtplugin.ScalaNativePlugin

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

    val jsProjects = projects.filter(p => Plugins.satisfied(p.plugins, Set(ScalaJSPlugin)))

    val nativeProjects =
      projects.filter(p => Plugins.satisfied(p.plugins, Set(ScalaNativePlugin)))

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

object TypelevelCiCrossPlugin extends AutoPlugin {
  override def requires = TypelevelCiPlugin

  override def buildSettings = Seq(
    githubWorkflowBuild := Seq(WorkflowStep.Sbt(List(s"$${{ matrix.ci }}"))),
    githubWorkflowBuildMatrixAdditions += "ci" -> Nil,
    githubWorkflowGeneratedUploadSteps ~= { steps =>
      // hack hack hack until upstreamed
      // workaround for https://github.com/djspiewak/sbt-github-actions/pull/66
      val mkdirStep = steps.headOption match {
        case Some(
              WorkflowStep
                .Run(command :: _, Some("Compress target directories"), _, _, _, _)) =>
          WorkflowStep.Run(
            commands = List(command.replace("tar cf targets.tar", "mkdir -p")),
            name = Some("Make target directories")
          )
        case _ => sys.error("Can't generate make target dirs workflow step")
      }
      mkdirStep +: steps
    }
  )
}

object TypelevelCiJVMPlugin extends AutoPlugin {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = addCommandAlias(
    "ciJVM",
    List(
      "project rootJVM",
      "clean",
      "test",
      "mimaReportBinaryIssues"
    ).mkString("; ", "; ", "")
  ) ++ Seq(
    githubWorkflowBuildMatrixAdditions ~= { matrix =>
      matrix.updated("ci", matrix("ci") ::: "ciJVM" :: Nil)
    }
  )
}

object TypelevelCiJSPlugin extends AutoPlugin {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = addCommandAlias(
    "ciJS",
    List(
      "project rootJS",
      "clean",
      "Test/fastOptJS",
      "test",
      "mimaReportBinaryIssues"
    ).mkString("; ", "; ", "")
  ) ++ Seq(
    githubWorkflowBuildMatrixAdditions ~= { matrix =>
      matrix.updated("ci", matrix("ci") ::: "ciJS" :: Nil)
    }
  )
}

object TypelevelCiNativePlugin extends AutoPlugin {
  override def requires = TypelevelCiCrossPlugin

  override def buildSettings: Seq[Setting[_]] = addCommandAlias(
    "ciNative",
    List(
      "project rootNative",
      "clean",
      "Test/nativeLink",
      "test",
      "mimaReportBinaryIssues"
    ).mkString("; ", "; ", "")
  ) ++ Seq(
    githubWorkflowBuildMatrixAdditions ~= { matrix =>
      matrix.updated("ci", matrix("ci") ::: "ciNative" :: Nil)
    }
  )
}
