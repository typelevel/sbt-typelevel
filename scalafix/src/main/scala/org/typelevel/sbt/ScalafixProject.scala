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
import sbtcompat.PluginCompat._
import scalafix.sbt._

import Keys._
import ScalafixTestkitPlugin.autoImport._

final class ScalafixProject private (
    val name: String,
    val rules: Project,
    val input: Project,
    val output: Project,
    val tests: Project
) extends CompositeProject {

  lazy val componentProjects = Seq(rules, input, output, tests)

  def componentProjectReferences =
    componentProjects.map(x => LocalProject(x.id): ProjectReference)

  def in(dir: File): ScalafixProject =
    new ScalafixProject(
      name,
      rules.in(dir / "rules"),
      input.in(dir / "input"),
      output.in(dir / "output"),
      tests.in(dir / "tests")
    )

  def rulesSettings(ss: Def.SettingsDefinition*): ScalafixProject =
    rulesConfigure(_.settings(ss*))

  def inputSettings(ss: Def.SettingsDefinition*): ScalafixProject =
    inputConfigure(_.settings(ss*))

  def outputSettings(ss: Def.SettingsDefinition*): ScalafixProject =
    outputConfigure(_.settings(ss*))

  def testsSettings(ss: Def.SettingsDefinition*): ScalafixProject =
    testsConfigure(_.settings(ss*))

  def rulesConfigure(transforms: (Project => Project)*): ScalafixProject =
    new ScalafixProject(
      name,
      rules.configure(transforms*),
      input,
      output,
      tests
    )

  def inputConfigure(transforms: (Project => Project)*): ScalafixProject =
    new ScalafixProject(
      name,
      rules,
      input.configure(transforms*),
      output,
      tests
    )

  def outputConfigure(transforms: (Project => Project)*): ScalafixProject =
    new ScalafixProject(
      name,
      rules,
      input,
      output.configure(transforms*),
      tests
    )

  def testsConfigure(transforms: (Project => Project)*): ScalafixProject =
    new ScalafixProject(
      name,
      rules,
      input,
      output,
      tests.configure(transforms*)
    )

}

object ScalafixProject {
  def apply(name: String): ScalafixProject = {

    lazy val rules = Project(s"$name-rules", file(s"$name/rules")).settings(
      libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % _root_
        .scalafix
        .sbt
        .BuildInfo
        .scalafixVersion
    )

    lazy val input =
      Project(s"$name-input", file(s"$name/input")).enablePlugins(NoPublishPlugin)

    lazy val output =
      Project(s"$name-output", file(s"$name/output")).enablePlugins(NoPublishPlugin)

    lazy val tests = Project(s"$name-tests", file(s"$name/tests"))
      .settings(
        scalafixTestkitOutputSourceDirectories := (LocalProject(
          output.id) / Compile / unmanagedSourceDirectories).value,
        scalafixTestkitInputSourceDirectories := (LocalProject(
          input.id) / Compile / unmanagedSourceDirectories).value,
        scalafixTestkitInputClasspath := Def.uncached {
          (LocalProject(input.id) / Compile / fullClasspath).value
        },
        scalafixTestkitInputScalacOptions := (LocalProject(
          input.id) / Compile / scalacOptions).value,
        scalafixTestkitInputScalaVersion := (LocalProject(
          input.id) / Compile / scalaVersion).value
      )
      .dependsOn(LocalProject(rules.id))
      .enablePlugins(NoPublishPlugin, ScalafixTestkitPlugin)

    new ScalafixProject(name, rules, input, output, tests)
  }
}
