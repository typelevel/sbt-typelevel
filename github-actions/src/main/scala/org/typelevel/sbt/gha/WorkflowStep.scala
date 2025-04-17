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

sealed abstract class WorkflowStep extends Product with Serializable {
  def id: Option[String]
  def name: Option[String]
  def cond: Option[String]
  def env: Map[String, String]
  def timeoutMinutes: Option[Int]
  def continueOnError: Boolean

  def withId(id: Option[String]): WorkflowStep
  def withName(name: Option[String]): WorkflowStep
  def withCond(cond: Option[String]): WorkflowStep
  def withEnv(env: Map[String, String]): WorkflowStep
  def withTimeoutMinutes(minutes: Option[Int]): WorkflowStep
  def withContinueOnError(continueOnError: Boolean): WorkflowStep

  def updatedEnv(name: String, value: String): WorkflowStep
  def concatEnv(env: TraversableOnce[(String, String)]): WorkflowStep
}

object WorkflowStep {

  val CheckoutFull: WorkflowStep = Use(
    UseRef.Public("actions", "checkout", "v4"),
    name = Some("Checkout current branch (full)"),
    params = Map("fetch-depth" -> "0"))

  val Checkout: WorkflowStep = Use(
    UseRef.Public("actions", "checkout", "v4"),
    name = Some("Checkout current branch (fast)"))

  val SetupSbt: WorkflowStep = WorkflowStep.Use(
    UseRef.Public("sbt", "setup-sbt", "v1"),
    name = Some("Setup sbt")
  )

  def SetupJava(versions: List[JavaSpec], enableCaching: Boolean = true): List[WorkflowStep] = {
    def sbtUpdateStep(cond: String, setupId: String) =
      if (enableCaching)
        List(
          WorkflowStep.Sbt(
            List("+update"),
            name = Some(s"sbt update"),
            cond = Some(s"$cond && steps.${setupId}.outputs.cache-hit == 'false'"),
            preamble = false
          ))
      else Nil

    val sbtCacheParams = if (enableCaching) Map("cache" -> "sbt") else Map.empty

    versions flatMap {
      case jv @ JavaSpec(JavaSpec.Distribution.GraalVM(graalVersion), javaVersion) =>
        val setupId = s"setup-graalvm-${graalVersion}-$javaVersion".replace('.', '_')
        val cond = s"matrix.java == '${jv.render}'"
        WorkflowStep.Use(
          UseRef.Public("graalvm", "setup-graalvm", "v1"),
          name = Some(s"Setup GraalVM (${jv.render})"),
          id = Some(setupId),
          cond = Some(cond),
          params =
            Map("version" -> graalVersion, "java-version" -> javaVersion) ++ sbtCacheParams
        ) +: sbtUpdateStep(cond, setupId)

      case jv @ JavaSpec(dist, version) =>
        val setupId = s"setup-java-${dist.rendering}-$version".replace('.', '_')
        val cond = s"matrix.java == '${jv.render}'"

        WorkflowStep.Use(
          UseRef.Public("actions", "setup-java", "v4"),
          name = Some(s"Setup Java (${jv.render})"),
          id = Some(setupId),
          cond = Some(cond),
          params =
            Map("distribution" -> dist.rendering, "java-version" -> version) ++ sbtCacheParams
        ) +: sbtUpdateStep(cond, setupId)
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

  sealed abstract class Run extends WorkflowStep {
    def commands: List[String]
    def params: Map[String, String]
    def workingDirectory: Option[String]

    def withCommands(commands: List[String]): Run
    def withParams(params: Map[String, String]): Run
    def withWorkingDirectory(workingDirectory: Option[String]): Run

    def updatedEnv(name: String, value: String): Run
    def concatEnv(env: TraversableOnce[(String, String)]): Run
    def updatedParams(name: String, value: String): Run
    def concatParams(params: TraversableOnce[(String, String)]): Run
  }

  object Run {
    def apply(
        commands: List[String],
        id: Option[String] = None,
        name: Option[String] = None,
        cond: Option[String] = None,
        env: Map[String, String] = Map(),
        params: Map[String, String] = Map(),
        timeoutMinutes: Option[Int] = None,
        workingDirectory: Option[String] = None,
        continueOnError: Boolean = false): Run =
      Impl(
        commands,
        id,
        name,
        cond,
        env,
        params,
        timeoutMinutes,
        workingDirectory,
        continueOnError)

    private final case class Impl(
        commands: List[String],
        id: Option[String],
        name: Option[String],
        cond: Option[String],
        env: Map[String, String],
        params: Map[String, String],
        timeoutMinutes: Option[Int],
        workingDirectory: Option[String],
        continueOnError: Boolean)
        extends Run {
      override def productPrefix = "Run"
      // scalafmt: { maxColumn = 200 }
      def withId(id: Option[String]) = copy(id = id)
      def withName(name: Option[String]) = copy(name = name)
      def withCond(cond: Option[String]) = copy(cond = cond)
      def withEnv(env: Map[String, String]) = copy(env = env)
      def withTimeoutMinutes(minutes: Option[Int]) = copy(timeoutMinutes = minutes)
      def withContinueOnError(continueOnError: Boolean) = copy(continueOnError = continueOnError)

      def withCommands(commands: List[String]) = copy(commands = commands)
      def withParams(params: Map[String, String]) = copy(params = params)
      def withWorkingDirectory(workingDirectory: Option[String]) = copy(workingDirectory = workingDirectory)

      def updatedEnv(name: String, value: String) = copy(env = this.env.updated(name, value))
      def concatEnv(env: TraversableOnce[(String, String)]) = copy(env = this.env ++ env)
      def updatedParams(name: String, value: String) = copy(params = this.params.updated(name, value))
      def concatParams(params: TraversableOnce[(String, String)]) = copy(params = this.params ++ params)
      // scalafmt: { maxColumn = 96 }
    }
  }

  sealed abstract class Sbt extends WorkflowStep {
    def commands: List[String]
    def params: Map[String, String]
    def preamble: Boolean

    def withCommands(commands: List[String]): Sbt
    def withParams(params: Map[String, String]): Sbt
    def withPreamble(preamble: Boolean): Sbt

    def updatedEnv(name: String, value: String): Sbt
    def concatEnv(env: TraversableOnce[(String, String)]): Sbt
    def updatedParams(name: String, value: String): Sbt
    def concatParams(params: TraversableOnce[(String, String)]): Sbt
  }

  object Sbt {
    def apply(
        commands: List[String],
        id: Option[String] = None,
        name: Option[String] = None,
        cond: Option[String] = None,
        env: Map[String, String] = Map(),
        params: Map[String, String] = Map(),
        timeoutMinutes: Option[Int] = None,
        preamble: Boolean = true,
        continueOnError: Boolean = false): Sbt =
      Impl(commands, id, name, cond, env, params, timeoutMinutes, preamble, continueOnError)

    private final case class Impl(
        commands: List[String],
        id: Option[String],
        name: Option[String],
        cond: Option[String],
        env: Map[String, String],
        params: Map[String, String],
        timeoutMinutes: Option[Int],
        preamble: Boolean,
        continueOnError: Boolean)
        extends Sbt {
      override def productPrefix = "Sbt"
      // scalafmt: { maxColumn = 200 }
      def withId(id: Option[String]) = copy(id = id)
      def withName(name: Option[String]) = copy(name = name)
      def withCond(cond: Option[String]) = copy(cond = cond)
      def withEnv(env: Map[String, String]) = copy(env = env)
      def withTimeoutMinutes(minutes: Option[Int]) = copy(timeoutMinutes = minutes)
      def withContinueOnError(continueOnError: Boolean): WorkflowStep = copy(continueOnError = continueOnError)

      def withCommands(commands: List[String]) = copy(commands = commands)
      def withParams(params: Map[String, String]) = copy(params = params)
      def withPreamble(preamble: Boolean) = copy(preamble = preamble)

      def updatedEnv(name: String, value: String) = copy(env = this.env.updated(name, value))
      def concatEnv(env: TraversableOnce[(String, String)]) = copy(env = this.env ++ env)
      def updatedParams(name: String, value: String) = copy(params = params.updated(name, value))
      def concatParams(params: TraversableOnce[(String, String)]) = copy(params = this.params ++ params)
      // scalafmt: { maxColumn = 96 }
    }
  }

  sealed abstract class Use extends WorkflowStep {
    def ref: UseRef
    def params: Map[String, String]

    def withRef(ref: UseRef): Use
    def withParams(params: Map[String, String]): Use

    def updatedEnv(name: String, value: String): Use
    def concatEnv(env: TraversableOnce[(String, String)]): Use
    def updatedParams(name: String, value: String): Use
    def concatParams(params: TraversableOnce[(String, String)]): Use
  }

  object Use {

    def apply(
        ref: UseRef,
        params: Map[String, String] = Map(),
        id: Option[String] = None,
        name: Option[String] = None,
        cond: Option[String] = None,
        env: Map[String, String] = Map(),
        timeoutMinutes: Option[Int] = None,
        continueOnError: Boolean = false): Use =
      Impl(ref, params, id, name, cond, env, timeoutMinutes, continueOnError)

    private final case class Impl(
        ref: UseRef,
        params: Map[String, String],
        id: Option[String],
        name: Option[String],
        cond: Option[String],
        env: Map[String, String],
        timeoutMinutes: Option[Int],
        continueOnError: Boolean)
        extends Use {
      override def productPrefix = "Use"
      // scalafmt: { maxColumn = 200 }
      def withId(id: Option[String]) = copy(id = id)
      def withName(name: Option[String]) = copy(name = name)
      def withCond(cond: Option[String]) = copy(cond = cond)
      def withEnv(env: Map[String, String]) = copy(env = env)
      def withTimeoutMinutes(minutes: Option[Int]) = copy(timeoutMinutes = minutes)
      def withContinueOnError(continueOnError: Boolean) = copy(continueOnError = continueOnError)

      def withRef(ref: UseRef) = copy(ref = ref)
      def withParams(params: Map[String, String]) = copy(params = params)

      def updatedEnv(name: String, value: String) = copy(env = this.env.updated(name, value))
      def concatEnv(env: TraversableOnce[(String, String)]) = copy(env = this.env ++ env)
      def updatedParams(name: String, value: String) = copy(params = params.updated(name, value))
      def concatParams(params: TraversableOnce[(String, String)]) = copy(params = this.params ++ params)
      // scalafmt: { maxColumn = 96 }
    }
  }
}
