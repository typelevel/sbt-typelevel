package org.typelevel.sbt

import sbt._
import sbtprojectmatrix.ProjectMatrix   // sbt‑2 API

/** Compatibility shim – keeps the old API but uses ProjectMatrix under the hood. */
@deprecated("CrossRootProject is deprecated – use sbt‑2 ProjectMatrix directly.", "0.6.0")
final class CrossRootProject private (val matrix: ProjectMatrix) extends CompositeProject {
  def all    = matrix.project("all")
  def jvm    = matrix.project("jvm")
  def js     = matrix.project("js")
  def native = matrix.project("native")

  override def componentProjects: Seq[Project] = Seq(all, jvm, js, native)

  def settings(ss: Def.SettingsDefinition*): CrossRootProject = new CrossRootProject(matrix.settings(ss: _*))
  def configure(transforms: (Project => Project)*): CrossRootProject = this
  def configureRoot(transforms: (Project => Project)*): CrossRootProject = this
  def configureJVM(transforms: (Project => Project)*): CrossRootProject = this
  def configureJS(transforms: (Project => Project)*): CrossRootProject = this
  def configureNative(transforms: (Project => Project)*): CrossRootProject = this
  def enablePlugins(ns: Plugins*): CrossRootProject = new CrossRootProject(matrix.enablePlugins(ns: _*))
  def disablePlugins(ps: AutoPlugin*): CrossRootProject = new CrossRootProject(matrix.disablePlugins(ps: _*))
  def aggregate(projects: CompositeProject*): CrossRootProject = this
}

object CrossRootProject {
  /** Factory used by the existing macro (we’ll drop the macro later). */
  def apply(id: String): CrossRootProject = {
    val pm = ProjectMatrix(id)
      .jvm(   Project(id + "JVM",   file(".jvm"))   )
      .js(    Project(id + "JS",    file(".js"))    )
      .native(Project(id + "Native",file(".native")))
      .all(   Project(id + "All",   file(".")))
    new CrossRootProject(pm)
  }
}
