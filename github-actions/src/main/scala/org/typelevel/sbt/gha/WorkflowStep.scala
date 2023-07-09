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

package org.typelevel.sbt.gha

import java.nio.file.FileSystems
import java.nio.file.Path

sealed trait WorkflowStep extends Product with Serializable {
  def id: Option[String]
  def name: Option[String]
  def cond: Option[String]
  def env: Map[String, String]
  def timeoutMinutes: Option[Int]

  def withId(id: Option[String]): WorkflowStep
  def withName(name: Option[String]): WorkflowStep
  def withCond(cond: Option[String]): WorkflowStep
  def withEnv(env: Map[String, String]): WorkflowStep
  def withTimeoutMinutes(minutes: Option[Int]): WorkflowStep
}

object WorkflowStep {

  val CheckoutFull: WorkflowStep = Use(
    UseRef.Public("actions", "checkout", "v3"),
    name = Some("Checkout current branch (full)"),
    params = Map("fetch-depth" -> "0"))

  val Checkout: WorkflowStep = Use(
    UseRef.Public("actions", "checkout", "v3"),
    name = Some("Checkout current branch (fast)"))

  def SetupJava(versions: List[JavaSpec]): List[WorkflowStep] = {
    def sbtUpdateStep(cond: String, setupId: String) =
      WorkflowStep.Sbt(
        List("reload", "+update"),
        name = Some(s"sbt update"),
        cond = Some(s"$cond && steps.${setupId}.outputs.cache-hit == 'false'")
      )

    val SetupJavaAction = UseRef.Public("actions", "setup-java", "v3")
    val SetupGraalVMAction = UseRef.Public("graalvm", "setup-graalvm", "v1")

    versions flatMap {
      case jv @ JavaSpec(JavaSpec.Distribution.GraalVM(graalVersion), javaVersion) =>
        val setupId = s"setup-graalvm-${graalVersion}-$javaVersion".replace('.', '_')
        val cond = s"matrix.java == '${jv.render}'"
        WorkflowStep.Use(
          SetupGraalVMAction,
          name = Some(s"Setup GraalVM (${jv.render})"),
          id = Some(setupId),
          cond = Some(cond),
          params =
            Map("version" -> graalVersion, "java-version" -> javaVersion, "cache" -> "sbt")
        ) :: sbtUpdateStep(cond, setupId) :: Nil

      case jv @ JavaSpec(dist, version) =>
        val setupId = s"setup-java-${dist.rendering}-$version".replace('.', '_')
        val cond = s"matrix.java == '${jv.render}'"

        WorkflowStep.Use(
          if (dist == JavaSpec.Distribution.GraalVM) SetupGraalVMAction else SetupJavaAction,
          name = Some(s"Setup Java (${jv.render})"),
          id = Some(setupId),
          cond = Some(cond),
          params =
            Map("distribution" -> dist.rendering, "java-version" -> version, "cache" -> "sbt")
        ) :: sbtUpdateStep(cond, setupId) :: Nil
    }

  }

  val Tmate: WorkflowStep =
    Use(UseRef.Public("mxschmitt", "action-tmate", "v3"), name = Some("Setup tmate session"))

  def DependencySubmission(
      workingDirectory: Option[String],
      modulesIgnore: Option[List[String]],
      configsIgnore: Option[List[String]],
      token: Option[String]
  ): WorkflowStep =
    Use(
      UseRef.Public("scalacenter", "sbt-dependency-submission", "v2"),
      name = Some("Submit Dependencies"),
      params = workingDirectory.map("working-directory" -> _).toMap ++
        modulesIgnore.filter(_.nonEmpty).map(m => "modules-ignore" -> m.mkString(" ")).toMap ++
        configsIgnore.filter(_.nonEmpty).map(c => "configs-ignore" -> c.mkString(" ")).toMap ++
        token.map("token" -> _).toMap
    )

  def ComputeVar(name: String, cmd: String): WorkflowStep =
    Run(
      List("echo \"" + name + "=$(" + cmd + ")\" >> $GITHUB_ENV"),
      name = Some(s"Export $name"))

  def ComputePrependPATH(cmd: String): WorkflowStep =
    Run(
      List("echo \"$(" + cmd + ")\" >> $GITHUB_PATH"),
      name = Some(s"Prepend $$PATH using $cmd"))

  final case class Run(
      commands: List[String],
      id: Option[String] = None,
      name: Option[String] = None,
      cond: Option[String] = None,
      env: Map[String, String] = Map(),
      params: Map[String, String] = Map(),
      timeoutMinutes: Option[Int] = None)
      extends WorkflowStep {
    def withId(id: Option[String]) = copy(id = id)
    def withName(name: Option[String]) = copy(name = name)
    def withCond(cond: Option[String]) = copy(cond = cond)
    def withEnv(env: Map[String, String]) = copy(env = env)
    def withTimeoutMinutes(minutes: Option[Int]) = copy(timeoutMinutes = minutes)
  }

  final case class Sbt(
      commands: List[String],
      id: Option[String] = None,
      name: Option[String] = None,
      cond: Option[String] = None,
      env: Map[String, String] = Map(),
      params: Map[String, String] = Map(),
      timeoutMinutes: Option[Int] = None)
      extends WorkflowStep {
    def withId(id: Option[String]) = copy(id = id)
    def withName(name: Option[String]) = copy(name = name)
    def withCond(cond: Option[String]) = copy(cond = cond)
    def withEnv(env: Map[String, String]) = copy(env = env)
    def withTimeoutMinutes(minutes: Option[Int]) = copy(timeoutMinutes = minutes)
  }

  final case class Use(
      ref: UseRef,
      params: Map[String, String] = Map(),
      id: Option[String] = None,
      name: Option[String] = None,
      cond: Option[String] = None,
      env: Map[String, String] = Map(),
      timeoutMinutes: Option[Int] = None)
      extends WorkflowStep {
    def withId(id: Option[String]) = copy(id = id)
    def withName(name: Option[String]) = copy(name = name)
    def withCond(cond: Option[String]) = copy(cond = cond)
    def withEnv(env: Map[String, String]) = copy(env = env)
    def withTimeoutMinutes(minutes: Option[Int]) = copy(timeoutMinutes = minutes)
  }

  def upload(paths: List[Path], artifactId: String): List[WorkflowStep] = {
    val pathStrs = paths.map(path => normalizeSeparators(path.toString))

    val sanitized = pathStrs.map { str =>
      if (str.indexOf(' ') >= 0) // TODO be less naive
        s"'$str'"
      else
        str
    }

    val mkdir = WorkflowStep.Run(
      List(s"mkdir -p ${sanitized.mkString(" ")}"),
      name = Some("Make target directories"))

    val tar = WorkflowStep.Run(
      List(s"tar cf targets.tar ${sanitized.mkString(" ")}"),
      name = Some("Compress target directories"))

    val upload = WorkflowStep.Use(
      UseRef.Public("actions", "upload-artifact", "v3"),
      name = Some(s"Upload target directories"),
      params = Map("name" -> artifactId, "path" -> "targets.tar")
    )

    List(mkdir, tar, upload)
  }

  private val PlatformSep = FileSystems.getDefault.getSeparator
  private def normalizeSeparators(pathStr: String): String = {
    pathStr.replace(PlatformSep, "/") // *force* unix separators
  }
}
