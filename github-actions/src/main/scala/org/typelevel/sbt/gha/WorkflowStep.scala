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

sealed trait WorkflowStep extends Product with Serializable {
  def id: Option[String]
  def name: Option[String]
  def cond: Option[String]
  def env: Map[String, String]

  def withId(id: Option[String]): WorkflowStep
  def withName(name: Option[String]): WorkflowStep
  def withCond(cond: Option[String]): WorkflowStep
  def withEnv(env: Map[String, String]): WorkflowStep
}

object WorkflowStep {

  val CheckoutFull: WorkflowStep = Use(
    UseRef.Public("actions", "checkout", "v3"),
    name = Some("Checkout current branch (full)"),
    params = Map("fetch-depth" -> "0"))

  val Checkout: WorkflowStep = Use(
    UseRef.Public("actions", "checkout", "v3"),
    name = Some("Checkout current branch (fast)"))

  def SetupJava(versions: List[JavaSpec]): List[WorkflowStep] =
    versions flatMap {
      case jv @ JavaSpec(JavaSpec.Distribution.GraalVM(graalVersion), version) =>
        WorkflowStep.Use(
          UseRef.Public("DeLaGuardo", "setup-graalvm", "5.0"),
          name = Some(s"Setup GraalVM (${jv.render})"),
          cond = Some(s"matrix.java == '${jv.render}'"),
          params = Map("graalvm" -> graalVersion, "java" -> s"java$version")
        ) :: Nil

      case jv @ JavaSpec(dist, version) if dist.isTlIndexed =>
        val cond = Some(s"matrix.java == '${jv.render}'")
        val id = s"download-java-${dist.rendering}-$version"
        List(
          WorkflowStep.Use(
            UseRef.Public("typelevel", "download-java", "v2"),
            name = Some(s"Download Java (${jv.render})"),
            id = Some(id),
            cond = cond,
            params = Map("distribution" -> dist.rendering, "java-version" -> version)
          ),
          WorkflowStep.Use(
            UseRef.Public("actions", "setup-java", "v3"),
            name = Some(s"Setup Java (${jv.render})"),
            cond = cond,
            params = Map(
              "distribution" -> "jdkfile",
              "java-version" -> version,
              "jdkFile" -> s"$${{ steps.$id.outputs.jdkFile }}"
            )
          )
        )

      case jv @ JavaSpec(dist, version) =>
        WorkflowStep.Use(
          UseRef.Public("actions", "setup-java", "v3"),
          name = Some(s"Setup Java (${jv.render})"),
          cond = Some(s"matrix.java == '${jv.render}'"),
          params = Map("distribution" -> dist.rendering, "java-version" -> version)
        ) :: Nil
    }

  val Tmate: WorkflowStep =
    Use(UseRef.Public("mxschmitt", "action-tmate", "v3"), name = Some("Setup tmate session"))

  val DependencySubmission: WorkflowStep =
    Use(
      UseRef.Public("scalacenter", "sbt-dependency-submission", "v2"),
      name = Some("Submit Dependencies")
    )

  val DependencyReview: WorkflowStep =
    Use(
      UseRef.Public("actions", "dependency-review-action", "v2"),
      name = Some("Review Dependencies")
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
      params: Map[String, String] = Map())
      extends WorkflowStep {
    def withId(id: Option[String]) = copy(id = id)
    def withName(name: Option[String]) = copy(name = name)
    def withCond(cond: Option[String]) = copy(cond = cond)
    def withEnv(env: Map[String, String]) = copy(env = env)
  }

  final case class Sbt(
      commands: List[String],
      id: Option[String] = None,
      name: Option[String] = None,
      cond: Option[String] = None,
      env: Map[String, String] = Map(),
      params: Map[String, String] = Map())
      extends WorkflowStep {
    def withId(id: Option[String]) = copy(id = id)
    def withName(name: Option[String]) = copy(name = name)
    def withCond(cond: Option[String]) = copy(cond = cond)
    def withEnv(env: Map[String, String]) = copy(env = env)
  }

  final case class Use(
      ref: UseRef,
      params: Map[String, String] = Map(),
      id: Option[String] = None,
      name: Option[String] = None,
      cond: Option[String] = None,
      env: Map[String, String] = Map())
      extends WorkflowStep {
    def withId(id: Option[String]) = copy(id = id)
    def withName(name: Option[String]) = copy(name = name)
    def withCond(cond: Option[String]) = copy(cond = cond)
    def withEnv(env: Map[String, String]) = copy(env = env)
  }
}
