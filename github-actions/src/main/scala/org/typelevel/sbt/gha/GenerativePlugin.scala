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

import org.typelevel.sbt.gha.WorkflowTrigger.BranchesFilter
import org.typelevel.sbt.gha.WorkflowTrigger.TagsFilter
import sbt.Keys._
import sbt._

import java.nio.file.FileSystems

object GenerativePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends GenerativeKeys {
    type WorkflowJob = org.typelevel.sbt.gha.WorkflowJob
    val WorkflowJob = org.typelevel.sbt.gha.WorkflowJob

    type Concurrency = org.typelevel.sbt.gha.Concurrency
    val Concurrency = org.typelevel.sbt.gha.Concurrency

    type JobContainer = org.typelevel.sbt.gha.JobContainer
    val JobContainer = org.typelevel.sbt.gha.JobContainer

    type WorkflowStep = org.typelevel.sbt.gha.WorkflowStep
    val WorkflowStep = org.typelevel.sbt.gha.WorkflowStep

    type RefPredicate = org.typelevel.sbt.gha.RefPredicate
    val RefPredicate = org.typelevel.sbt.gha.RefPredicate

    type Ref = org.typelevel.sbt.gha.Ref
    val Ref = org.typelevel.sbt.gha.Ref

    type UseRef = org.typelevel.sbt.gha.UseRef
    val UseRef = org.typelevel.sbt.gha.UseRef

    type PREventType = org.typelevel.sbt.gha.PREventType
    val PREventType = org.typelevel.sbt.gha.PREventType

    type MatrixInclude = org.typelevel.sbt.gha.MatrixInclude
    val MatrixInclude = org.typelevel.sbt.gha.MatrixInclude

    type MatrixExclude = org.typelevel.sbt.gha.MatrixExclude
    val MatrixExclude = org.typelevel.sbt.gha.MatrixExclude

    type Paths = org.typelevel.sbt.gha.Paths
    val Paths = org.typelevel.sbt.gha.Paths

    type JavaSpec = org.typelevel.sbt.gha.JavaSpec
    val JavaSpec = org.typelevel.sbt.gha.JavaSpec
  }

  import autoImport._

  private object MatrixKeys {
    final val OS = "os"
    final val Scala = "scala"
    final val Java = "java"

    def groupId(keys: List[String]): String =
      (MatrixKeys.OS :: MatrixKeys.Java :: MatrixKeys.Scala :: keys)
        .map(k => s"$${{ matrix.$k }}")
        .mkString("-")
  }

  private def indent(output: String, level: Int): String = {
    val space = (0 until level * 2).map(_ => ' ').mkString
    output.replaceAll("(?m)^", space).replaceAll("""\n[ ]+\n""", "\n\n")
  }

  private def isSafeString(str: String): Boolean =
    !(str.indexOf(':') >= 0 || // pretend colon is illegal everywhere for simplicity
      str.indexOf('#') >= 0 || // same for comment
      str.indexOf('!') == 0 ||
      str.indexOf('*') == 0 ||
      str.indexOf('-') == 0 ||
      str.indexOf('?') == 0 ||
      str.indexOf('{') == 0 ||
      str.indexOf('}') == 0 ||
      str.indexOf('[') >= 0 ||
      str.indexOf(']') >= 0 ||
      str.indexOf(',') == 0 ||
      str.indexOf('|') == 0 ||
      str.indexOf('>') == 0 ||
      str.indexOf('@') == 0 ||
      str.indexOf('`') == 0 ||
      str.indexOf('"') == 0 ||
      str.indexOf('\'') == 0 ||
      str.indexOf('&') == 0)

  private def wrap(str: String): String =
    if (str.indexOf('\n') >= 0)
      "|\n" + indent(str, 1)
    else if (isSafeString(str))
      str
    else
      s"'${str.replace("'", "''")}'"

  def compileOn(on: List[WorkflowTrigger]): String = {
    def renderList(field: String, values: List[String]): String =
      s"$field:${compileList(values, 1)}\n"
    def renderBranchesFilter(filter: Option[BranchesFilter]) =
      filter.fold("") {
        case BranchesFilter.Branches(branches) if branches.size != 0 =>
          renderList("branches", branches)
        case BranchesFilter.BranchesIgnore(branches) if branches.size != 0 =>
          renderList("branches-ignore", branches)
        case _ => ""
      }
    def renderTypes(prEventTypes: List[PREventType]) =
      if (prEventTypes.sortBy(_.toString) == PREventType.Defaults) ""
      else renderList("types", prEventTypes.map(compilePREventType))
    def renderTagsFilter(filter: Option[TagsFilter]) =
      filter.fold("") {
        case TagsFilter.Tags(tags) if tags.size != 0 =>
          renderList("tags", tags)
        case TagsFilter.TagsIgnore(tags) if tags.size != 0 =>
          renderList("tags-ignore", tags)
        case _ => ""
      }
    def renderPaths(paths: Paths) = paths match {
      case Paths.None => ""
      case Paths.Include(paths) => renderList("paths", paths)
      case Paths.Ignore(paths) => renderList("paths-ignore", paths)
    }

    import WorkflowTrigger._
    val renderedTriggers =
      on.map {
        case pr: WorkflowTrigger.PullRequest =>
          val renderedBranches = renderBranchesFilter(pr.branchesFilter)
          val renderedTypes = renderTypes(pr.types)
          val renderedPaths = renderPaths(pr.paths)
          val compose = renderedBranches + renderedTypes + renderedPaths
          "pull_request:\n" + indent(compose, 1)
        case push: WorkflowTrigger.Push =>
          val renderedBranchesFilter = renderBranchesFilter(push.branchesFilter)
          val renderedTagsFilter = renderTagsFilter(push.tagsFilter)
          val renderedPaths = renderPaths(push.paths)
          val compose = renderedBranchesFilter + renderedTagsFilter + renderedPaths
          "push:\n" + indent(compose, 1)
        case call: WorkflowTrigger.WorkflowCall =>
          if (call.inputs.size == 0) "workflow_call:\n"
          else {
            val renderedInputs = {
              def renderInput(id: String, i: WorkflowTrigger.WorkflowCallInput): String = {
                val rndrType = i.`type` match {
                  case WorkflowCallInputType.Boolean => "type: boolean\n"
                  case WorkflowCallInputType.Number => "type: number\n"
                  case WorkflowCallInputType.String => "type: string\n"
                }
                val rndrDescription = i.description.fold("")(d => s"description: $d\n")
                val rndrRequired = s"required: ${i.required}\n"
                val rndrDefault = i.default.fold("")(d => s"default: $d\n")
                s"$id:\n" + indent(rndrDescription + rndrRequired + rndrDefault + rndrType, 1)
              }
              "inputs:\n" + indent(call.inputs.map(renderInput _ tupled).mkString(""), 1)
            }
            "workflow_call:\n" + indent(renderedInputs, 1)
          }
        case dispatch: WorkflowTrigger.WorkflowDispatch =>
          val renderedInputs = {
            def renderInput(id: String, i: WorkflowTrigger.WorkflowDispatchInput): String = {
              val rndrType = i.`type` match {
                case WorkflowDispatchInputType.Boolean => "type: boolean\n"
                case WorkflowDispatchInputType.Number => "type: number\n"
                case WorkflowDispatchInputType.String => "type: string\n"
                case WorkflowDispatchInputType.Environment => "type: environment\n"
                case WorkflowDispatchInputType.Choice(options) =>
                  "type: choice\n" + indent(options.mkString("- ", "\n- ", "\n"), 1)
              }
              val rndrDescription = i.description.fold("")(d => s"description: $d\n")
              val rndrRequired = s"required: ${i.required}\n"
              val rndrDefault = i.default.fold("")(d => s"default: $d\n")
              s"$id:\n" + indent(rndrDescription + rndrRequired + rndrDefault + rndrType, 1)
            }
            "inputs:\n" + indent(dispatch.inputs.map(renderInput _ tupled).mkString("\n"), 1)
          }
          "workflow_dispatch:\n" + indent(renderedInputs, 1)
        case raw: WorkflowTrigger.Raw => raw.toYaml
      }.mkString("\n", "", "")

    "on:" + indent(renderedTriggers, 1)
  }

  def compileList(items: List[String], level: Int): String = {
    val rendered = items.map(wrap)
    if (rendered.map(_.length).sum < 40) // just arbitrarily...
      rendered.mkString(" [", ", ", "]")
    else
      "\n" + indent(rendered.map("- " + _).mkString("\n"), level)
  }

  def compileListOfSimpleDicts(items: List[Map[String, String]]): String =
    items map { dict =>
      val rendered = dict map { case (key, value) => s"$key: $value" } mkString "\n"

      "-" + indent(rendered, 1).substring(1)
    } mkString "\n"

  def compilePREventType(tpe: PREventType): String = {
    import PREventType._

    tpe match {
      case Assigned => "assigned"
      case Unassigned => "unassigned"
      case Labeled => "labeled"
      case Unlabeled => "unlabeled"
      case Opened => "opened"
      case Edited => "edited"
      case Closed => "closed"
      case Reopened => "reopened"
      case Synchronize => "synchronize"
      case ReadyForReview => "ready_for_review"
      case Locked => "locked"
      case Unlocked => "unlocked"
      case ReviewRequested => "review_requested"
      case ReviewRequestRemoved => "review_request_removed"
    }
  }

  def compileSecrets(secrets: Secrets): String = secrets match {
    case Secrets.Inherit => s"\nsecrets: inherit"
    case Secrets.Values(values) => compileMap(values, prefix = "\nsecrets")
  }

  def compileRef(ref: Ref): String = ref match {
    case Ref.Branch(name) => s"refs/heads/$name"
    case Ref.Tag(name) => s"refs/tags/$name"
  }

  def compileBranchPredicate(target: String, pred: RefPredicate): String = pred match {
    case RefPredicate.Equals(ref) =>
      s"$target == '${compileRef(ref)}'"

    case RefPredicate.Contains(Ref.Tag(name)) =>
      s"(startsWith($target, 'refs/tags/') && contains($target, '$name'))"

    case RefPredicate.Contains(Ref.Branch(name)) =>
      s"(startsWith($target, 'refs/heads/') && contains($target, '$name'))"

    case RefPredicate.StartsWith(ref) =>
      s"startsWith($target, '${compileRef(ref)}')"

    case RefPredicate.EndsWith(Ref.Tag(name)) =>
      s"(startsWith($target, 'refs/tags/') && endsWith($target, '$name'))"

    case RefPredicate.EndsWith(Ref.Branch(name)) =>
      s"(startsWith($target, 'refs/heads/') && endsWith($target, '$name'))"
  }

  def compileConcurrency(concurrency: Concurrency): String =
    concurrency.cancelInProgress match {
      case Some(value) =>
        val fields = s"""group: ${wrap(concurrency.group)}
                        |cancel-in-progress: ${wrap(value)}""".stripMargin
        s"""concurrency:
           |${indent(fields, 1)}""".stripMargin

      case None =>
        s"concurrency: ${wrap(concurrency.group)}"
    }

  def compileEnvironment(environment: JobEnvironment): String =
    environment.url match {
      case Some(url) =>
        val fields = s"""name: ${wrap(environment.name)}
                        |url: ${wrap(url.toString)}""".stripMargin
        s"""environment:
           |${indent(fields, 1)}""".stripMargin
      case None =>
        s"environment: ${wrap(environment.name)}"
    }

  def compileEnv(env: Map[String, String], prefix: String = "", suffix: String = ""): String =
    compileMap(env, prefix = s"${prefix}env", suffix = suffix)
  def compileMap(data: Map[String, String], prefix: String = "", suffix: String = ""): String =
    if (data.isEmpty) ""
    else {
      val rendered = data
        .map {
          case (key, value) =>
            if (!isSafeString(key) || key.indexOf(' ') >= 0)
              sys.error(s"'$key' is not a valid variable name")

            s"""$key: ${wrap(value)}"""
        }
        .mkString("\n")
      s"""$prefix:\n${indent(rendered, 1)}$suffix"""
    }

  def compilePermissionScope(permissionScope: PermissionScope): String = permissionScope match {
    case PermissionScope.Actions => "actions"
    case PermissionScope.Checks => "checks"
    case PermissionScope.Contents => "contents"
    case PermissionScope.Deployments => "deployments"
    case PermissionScope.IdToken => "id-token"
    case PermissionScope.Issues => "issues"
    case PermissionScope.Discussions => "discussions"
    case PermissionScope.Packages => "packages"
    case PermissionScope.Pages => "pages"
    case PermissionScope.PullRequests => "pull-requests"
    case PermissionScope.RepositoryProjects => "repository-projects"
    case PermissionScope.SecurityEvents => "security-events"
    case PermissionScope.Statuses => "statuses"
  }

  def compilePermissionsValue(permissionValue: PermissionValue): String =
    permissionValue match {
      case PermissionValue.Read => "read"
      case PermissionValue.Write => "write"
      case PermissionValue.None => "none"
    }

  def compilePermissions(
      permissions: Option[Permissions],
      prefix: String = "",
      suffix: String = ""): String = {
    permissions match {
      case Some(perms) =>
        val rendered = perms match {
          case Permissions.ReadAll => " read-all"
          case Permissions.WriteAll => " write-all"
          case Permissions.None => " {}"
          case x: Permissions.Specify =>
            val map = x.asMap.map {
              case (key, value) =>
                s"${compilePermissionScope(key)}: ${compilePermissionsValue(value)}"
            }
            "\n" + indent(map.mkString("\n"), 1)
        }
        s"${prefix}permissions:$rendered$suffix"

      case None => ""
    }
  }

  def compileStep(
      step: WorkflowStep,
      sbt: String,
      sbtStepPreamble: List[String],
      declareShell: Boolean = false): String = {
    import WorkflowStep._

    val renderedName = step.name.map(wrap).map("name: " + _ + "\n").getOrElse("")
    val renderedId = step.id.map(wrap).map("id: " + _ + "\n").getOrElse("")
    val renderedCond = step.cond.map(wrap).map("if: " + _ + "\n").getOrElse("")
    val renderedShell = if (declareShell) "shell: bash\n" else ""
    val renderedContinueOnError = if (step.continueOnError) "continue-on-error: true\n" else ""

    val renderedEnv = compileEnv(step.env, suffix = "\n")

    val renderedTimeoutMinutes =
      step.timeoutMinutes.map("timeout-minutes: " + _ + "\n").getOrElse("")

    val preamble: String =
      renderedName + renderedId + renderedCond + renderedEnv + renderedTimeoutMinutes

    val body = step match {
      case run: Run =>
        val renderedWorkingDirectory =
          run.workingDirectory.map(wrap).map("working-directory: " + _ + "\n").getOrElse("")
        renderRunBody(
          run.commands,
          run.params,
          renderedShell,
          renderedWorkingDirectory,
          renderedContinueOnError)

      case sbtStep: Sbt =>
        import sbtStep.commands

        val preamble = if (sbtStep.preamble) sbtStepPreamble else Nil
        val sbtClientMode = sbt.matches("""sbt.* --client($| .*)""")
        val safeCommands =
          if (sbtClientMode)
            s"'${(preamble ::: commands).mkString("; ")}'"
          else
            (preamble ::: commands)
              .map { c =>
                if (c.indexOf(' ') >= 0)
                  s"'$c'"
                else
                  c
              }
              .mkString(" ")

        renderRunBody(
          commands = List(s"$sbt $safeCommands"),
          params = sbtStep.params,
          renderedShell = renderedShell,
          renderedWorkingDirectory = "",
          renderedContinueOnError = renderedContinueOnError
        )

      case use: Use =>
        import use.{ref, params}

        val decl = ref match {
          case UseRef.Public(owner, repo, ref) =>
            s"uses: $owner/$repo@$ref"

          case UseRef.Local(path) =>
            val cleaned =
              if (path.startsWith("./"))
                path
              else
                "./" + path

            s"uses: $cleaned"

          case UseRef.Docker(image, tag, Some(host)) =>
            s"uses: docker://$host/$image:$tag"

          case UseRef.Docker(image, tag, None) =>
            s"uses: docker://$image:$tag"
        }

        decl + renderedContinueOnError + renderParams(params)
    }

    indent(preamble + body, 1).updated(0, '-')
  }

  @deprecated("Use the overload with renderedContinueOnError", since = "0.8.1")
  def renderRunBody(
      commands: List[String],
      params: Map[String, String],
      renderedShell: String,
      renderedWorkingDirectory: String): String =
    renderRunBody(commands, params, renderedShell, renderedWorkingDirectory, "")

  def renderRunBody(
      commands: List[String],
      params: Map[String, String],
      renderedShell: String,
      renderedWorkingDirectory: String,
      renderedContinueOnError: String): String =
    renderedShell + renderedWorkingDirectory + renderedContinueOnError + "run: " + wrap(
      commands.mkString("\n")) + renderParams(params)

  def renderParams(params: Map[String, String]): String =
    compileMap(params, prefix = "\nwith")

  def compileJob(job: WorkflowJob, sbt: String): String = job match {
    case job: WorkflowJob.Run => compileRunJob(job, sbt)
    case job: WorkflowJob.Use => compileUseJob(job)
  }
  def compileUseJob(job: WorkflowJob.Use): String = {
    val renderedNeeds =
      if (job.needs.isEmpty)
        ""
      else
        job.needs.mkString("\nneeds: [", ", ", "]")

    val renderedConcurrency =
      job.concurrency.map(compileConcurrency).map("\n" + _).getOrElse("")

    val renderedPermissions = compilePermissions(job.permissions, prefix = "\n")
    val renderedSecrets = job.secrets.fold("")(compileSecrets)

    val renderedOutputs = compileMap(job.outputs, prefix = "\noutputs")

    val renderedInputs = compileMap(job.params, prefix = "\nwith")

    // format: off
    val body = s"""name: ${wrap(job.name)}${renderedNeeds}${renderedConcurrency}
      |uses: ${job.uses}${renderedInputs}${renderedOutputs}${renderedSecrets}${renderedPermissions}
      |""".stripMargin
    // format: on

    s"${job.id}:\n${indent(body, 1)}"
  }

  def compileRunJob(job: WorkflowJob.Run, sbt: String): String = {
    val renderedNeeds =
      if (job.needs.isEmpty)
        ""
      else
        job.needs.mkString("\nneeds: [", ", ", "]")

    val renderedEnvironment =
      job.environment.map(compileEnvironment).map("\n" + _).getOrElse("")

    val renderedCond = job.cond.map(wrap).map("\nif: " + _).getOrElse("")

    val renderedConcurrency =
      job.concurrency.map(compileConcurrency).map("\n" + _).getOrElse("")

    val renderedContainer = job.container match {
      case Some(JobContainer(image, credentials, env, volumes, ports, options)) =>
        if (credentials.isEmpty && env.isEmpty && volumes.isEmpty && ports.isEmpty && options.isEmpty) {
          s"\ncontainer: ${wrap(image)}"
        } else {
          val renderedImage = s"image: ${wrap(image)}"

          val renderedCredentials = credentials match {
            case Some((username, password)) =>
              s"\ncredentials:\n${indent(s"username: ${wrap(username)}\npassword: ${wrap(password)}", 1)}"

            case None =>
              ""
          }

          val renderedEnv = compileEnv(env, prefix = "\n")

          val renderedVolumes =
            if (volumes.nonEmpty)
              s"\nvolumes:${compileList(volumes.toList map { case (l, r) => s"$l:$r" }, 1)}"
            else
              ""

          val renderedPorts =
            if (ports.nonEmpty)
              s"\nports:${compileList(ports.map(_.toString), 1)}"
            else
              ""

          val renderedOptions =
            if (options.nonEmpty)
              s"\noptions: ${wrap(options.mkString(" "))}"
            else
              ""

          s"\ncontainer:\n${indent(renderedImage + renderedCredentials + renderedEnv + renderedVolumes + renderedPorts + renderedOptions, 1)}"
        }

      case None =>
        ""
    }

    val renderedEnv = compileEnv(job.env, "\n")

    val renderedOutputs = compileMap(job.outputs, prefix = "\noutputs")

    val renderedPerm = compilePermissions(job.permissions, prefix = "\n")

    val renderedTimeoutMinutes =
      job.timeoutMinutes.map(timeout => s"\ntimeout-minutes: $timeout").getOrElse("")

    List("include", "exclude").foreach { key =>
      if (job.matrixAdds.contains(key)) {
        sys.error(s"key `$key` is reserved and cannot be used in an Actions matrix definition")
      }
    }

    val renderedMatricesAdds =
      if (job.matrixAdds.isEmpty) ""
      else
        job
          .matrixAdds
          .toList
          .sortBy(_._1)
          .map { case (key, values) => s"$key: ${values.map(wrap).mkString("[", ", ", "]")}" }
          .mkString("\n", "\n", "")

    // TODO refactor all of this stuff to use whitelist instead
    val whitelist = Map(
      MatrixKeys.OS -> job.oses,
      MatrixKeys.Scala -> job.scalas,
      MatrixKeys.Java -> job.javas.map(_.render)) ++ job.matrixAdds

    def checkMatching(matching: Map[String, String]): Unit = {
      matching foreach {
        case (key, value) =>
          if (!whitelist.contains(key)) {
            sys.error(s"inclusion key `$key` was not found in matrix")
          }

          if (!whitelist(key).contains(value)) {
            sys.error(
              s"inclusion key `$key` was present in matrix, but value `$value` was not in ${whitelist(key)}")
          }
      }
    }

    val renderedIncludes =
      if (job.matrixIncs.isEmpty) ""
      else {
        job.matrixIncs.foreach(inc => checkMatching(inc.matching))

        val rendered = compileListOfSimpleDicts(
          job.matrixIncs.map(i => i.matching ++ i.additions))

        s"\ninclude:\n${indent(rendered, 1)}"
      }

    val renderedExcludes =
      if (job.matrixExcs.isEmpty) ""
      else {
        job.matrixExcs.foreach(exc => checkMatching(exc.matching))

        val rendered = compileListOfSimpleDicts(job.matrixExcs.map(_.matching))

        s"\nexclude:\n${indent(rendered, 1)}"
      }

    val renderedMatrices = indent(
      buildMatrix(
        0,
        "os" -> job.oses,
        "scala" -> job.scalas,
        "java" -> job.javas.map(_.render)) +
        renderedMatricesAdds + renderedIncludes + renderedExcludes,
      2)

    val declareShell = job.oses.exists(_.contains("windows"))

    val runsOn =
      if (job.runsOnExtraLabels.isEmpty)
        s"$${{ matrix.os }}"
      else
        job.runsOnExtraLabels.mkString(s"""[ "$${{ matrix.os }}", """, ", ", " ]")

    val renderedFailFast = job.matrixFailFast.fold("")("\n  fail-fast: " + _)

    val renderedSteps = indent(
      job
        .steps
        .map(compileStep(_, sbt, job.sbtStepPreamble, declareShell = declareShell))
        .mkString("\n\n"),
      1)
    // format: off
    val body = s"""name: ${wrap(job.name)}${renderedNeeds}${renderedCond}
      |strategy:${renderedFailFast}
      |  matrix:
      |${renderedMatrices}
      |runs-on: ${runsOn}${renderedEnvironment}${renderedContainer}${renderedPerm}${renderedEnv}${renderedOutputs}${renderedConcurrency}${renderedTimeoutMinutes}
      |steps:
      |${renderedSteps}""".stripMargin
    // format: on

    s"${job.id}:\n${indent(body, 1)}"
  }

  private def buildMatrix(level: Int, prefixWithEntries: (String, List[String])*): String =
    prefixWithEntries
      .collect {
        case (prefix, entries) if entries.nonEmpty =>
          s"$prefix:${compileList(entries, 1)}"
      }
      .map(indent(_, level))
      .mkString("\n")

  private def toWorkflow(
      name: String,
      branches: List[String],
      tags: List[String],
      paths: Paths,
      prEventTypes: List[PREventType],
      permissions: Option[Permissions],
      env: Map[String, String],
      concurrency: Option[Concurrency],
      jobs: List[WorkflowJob]
  ): Workflow = {
    Workflow(
      on = List(
        WorkflowTrigger.PullRequest(
          branchesFilter =
            if (branches.isEmpty) None else Some(BranchesFilter.Branches(branches)),
          paths = paths,
          types = prEventTypes),
        WorkflowTrigger.Push(
          branchesFilter =
            if (branches.isEmpty) None else Some(BranchesFilter.Branches(branches)),
          tagsFilter = if (tags.isEmpty) None else Some(TagsFilter.Tags(tags)),
          paths = paths
        )
      )
    ).withName(Option(name))
      .withPermissions(permissions)
      .withEnv(env)
      .withConcurrency(concurrency)
      .withJobs(jobs)
  }

  def render(workflow: Workflow, sbt: String): String = {
    import workflow._

    val renderedName = name.fold("") { name => s"name: ${wrap(name)}" }

    val renderedEnv = compileEnv(env, suffix = "\n\n")
    val renderedPerm = compilePermissions(permissions, suffix = "\n\n")

    val renderedConcurrency =
      concurrency.map(compileConcurrency).map(_ + "\n\n").getOrElse("")

    val renderedOn = compileOn(on)

    val renderedJobs = "jobs:\n" + indent(jobs.map(compileJob(_, sbt)).mkString("\n\n"), 1)

    s"""# This file was automatically generated by sbt-github-actions using the
       |# githubWorkflowGenerate task. You should add and commit this file to
       |# your git repository. It goes without saying that you shouldn't edit
       |# this file by hand! Instead, if you wish to make changes, you should
       |# change your sbt build configuration to revise the workflow description
       |# to meet your needs, then regenerate this file.
       |
       |${renderedName}
       |
       |${renderedOn}
       |${renderedPerm}${renderedEnv}${renderedConcurrency}${renderedJobs}
       |""".stripMargin
  }

  val settingDefaults = Seq(
    githubWorkflowSbtCommand := "sbt",
    githubWorkflowIncludeClean := true,
    // This is currently set to false because of https://github.com/sbt/sbt/issues/6468. When a new SBT version is
    // released that fixes this issue then check for that SBT version (or higher) and set to true.
    githubWorkflowUseSbtThinClient := false,
    githubWorkflowConcurrency := Some(
      Concurrency(
        group = s"$${{ github.workflow }} @ $${{ github.ref }}",
        cancelInProgress = true)
    ),
    githubWorkflowBuildMatrixFailFast := None,
    githubWorkflowBuildMatrixAdditions := Map(),
    githubWorkflowBuildMatrixInclusions := Seq(),
    githubWorkflowBuildMatrixExclusions := Seq(),
    githubWorkflowBuildRunsOnExtraLabels := Seq(),
    githubWorkflowBuildTimeoutMinutes := Some(60),
    githubWorkflowBuildPreamble := Seq(),
    githubWorkflowBuildPostamble := Seq(),
    githubWorkflowBuildSbtStepPreamble := Seq(s"++ $${{ matrix.scala }}"),
    githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test"), name = Some("Build project"))),
    githubWorkflowPublishPreamble := Seq(),
    githubWorkflowPublishPostamble := Seq(),
    githubWorkflowPublish := Seq(
      WorkflowStep.Sbt(List("+publish"), name = Some("Publish project"))),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("main"))),
    githubWorkflowPublishCond := None,
    githubWorkflowPublishTimeoutMinutes := None,
    githubWorkflowPublishNeeds := Seq("build"),
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11")),
    githubWorkflowScalaVersions := {
      val scalas = crossScalaVersions.value
      val binaryScalas = scalas.map(CrossVersion.binaryScalaVersion(_))
      if (binaryScalas.toSet.size == scalas.size)
        binaryScalas
      else
        scalas
    },
    githubWorkflowOSes := Seq("ubuntu-22.04"),
    githubWorkflowDependencyPatterns := Seq("**/*.sbt", "project/build.properties"),
    githubWorkflowTargetBranches := Seq("**"),
    githubWorkflowTargetTags := Seq(),
    githubWorkflowTargetPaths := Paths.None,
    githubWorkflowEnv := Map("GITHUB_TOKEN" -> s"$${{ secrets.GITHUB_TOKEN }}"),
    githubWorkflowPermissions := None,
    githubWorkflowAddedJobs := Seq()
  )

  private lazy val internalTargetAggregation =
    settingKey[Seq[File]]("Aggregates target directories from all subprojects")

  private val macosGuard = Some("contains(runner.os, 'macos')")
  private val windowsGuard = Some("contains(runner.os, 'windows')")

  private val PlatformSep = FileSystems.getDefault.getSeparator
  private def normalizeSeparators(pathStr: String): String = {
    pathStr.replace(PlatformSep, "/") // *force* unix separators
  }

  private val pathStrs = Def.setting {
    val base = (ThisBuild / baseDirectory).value.toPath

    internalTargetAggregation.value map { file =>
      val path = file.toPath

      if (path.isAbsolute)
        normalizeSeparators(base.relativize(path).toString)
      else
        normalizeSeparators(path.toString)
    }
  }

  override def globalSettings =
    Seq(internalTargetAggregation := Seq(), githubWorkflowArtifactUpload := true)

  override def buildSettings = settingDefaults ++ Seq(
    githubWorkflowPREventTypes := PREventType.Defaults,
    githubWorkflowArtifactDownloadExtraKeys := Set.empty,
    githubWorkflowGeneratedUploadSteps := {
      val generate =
        githubWorkflowArtifactUpload.value &&
          githubWorkflowPublishTargetBranches.value.nonEmpty
      if (generate) {
        val sanitized = pathStrs.value map { str =>
          if (str.indexOf(' ') >= 0) // TODO be less naive
            s"'$str'"
          else
            str
        }

        val mkdir = WorkflowStep.Run(
          List(s"mkdir -p ${sanitized.mkString(" ")} project/target"),
          name = Some("Make target directories"),
          cond = Some(publicationCond.value))

        val tar = WorkflowStep.Run(
          List(s"tar cf targets.tar ${sanitized.mkString(" ")} project/target"),
          name = Some("Compress target directories"),
          cond = Some(publicationCond.value))

        val keys = githubWorkflowBuildMatrixAdditions.value.keys.toList.sorted
        val artifactId = MatrixKeys.groupId(keys)

        val upload = WorkflowStep.Use(
          UseRef.Public("actions", "upload-artifact", "v5"),
          name = Some(s"Upload target directories"),
          params = Map("name" -> s"target-$artifactId", "path" -> "targets.tar"),
          cond = Some(publicationCond.value)
        )

        Seq(mkdir, tar, upload)
      } else {
        Seq()
      }
    },
    githubWorkflowGeneratedDownloadSteps := {
      val extraKeys = githubWorkflowArtifactDownloadExtraKeys.value
      val additions = githubWorkflowBuildMatrixAdditions.value
      val matrixAdds = additions.map {
        case (key, values) =>
          if (extraKeys(key))
            key -> values // we want to iterate over all values
          else
            key -> values.take(1) // we only want the primary value
      }

      val oses = githubWorkflowOSes.value.toList.take(1)
      val scalas = githubWorkflowScalaVersions.value.toList
      val javas = githubWorkflowJavaVersions.value.toList.take(1)
      val exclusions = githubWorkflowBuildMatrixExclusions.value.toList

      // we build the list of artifacts, by iterating over all combinations of keys
      val artifacts =
        expandMatrix(
          oses,
          scalas,
          javas,
          matrixAdds,
          Nil,
          exclusions
        ).map {
          case _ :: scala :: _ :: tail => scala :: tail
          case _ => sys.error("Bug generating artifact download steps") // shouldn't happen
        }

      if (githubWorkflowArtifactUpload.value) {
        artifacts flatMap { v =>
          val pretty = v.mkString(", ")

          val download = WorkflowStep.Use(
            UseRef.Public("actions", "download-artifact", "v6"),
            name = Some(s"Download target directories ($pretty)"),
            params =
              Map("name" -> s"target-$${{ matrix.os }}-$${{ matrix.java }}-${v.mkString("-")}")
          )

          val untar = WorkflowStep.Run(
            List("tar xf targets.tar", "rm targets.tar"),
            name = Some(s"Inflate target directories ($pretty)"))

          Seq(download, untar)
        }
      } else {
        Seq()
      }
    },
    githubWorkflowGeneratedCacheSteps := Seq(),
    githubWorkflowJobSetup := {

      val autoCrlfOpt = if (githubWorkflowOSes.value.exists(_.contains("windows"))) {
        List(
          WorkflowStep.Run(
            List("git config --global core.autocrlf false"),
            name = Some("Ignore line ending differences in git"),
            cond = windowsGuard))
      } else {
        Nil
      }

      autoCrlfOpt :::
        List(WorkflowStep.CheckoutFull) :::
        WorkflowStep.SetupSbt ::
        WorkflowStep.SetupJava(githubWorkflowJavaVersions.value.toList) :::
        githubWorkflowGeneratedCacheSteps.value.toList
    },
    githubWorkflowBuildJob := {
      val uploadStepsOpt =
        if (githubWorkflowPublishTargetBranches.value.isEmpty &&
          githubWorkflowAddedJobs.value.isEmpty)
          Nil
        else
          githubWorkflowGeneratedUploadSteps.value.toList

      WorkflowJob.Run(
        "build",
        "Test",
        githubWorkflowJobSetup.value.toList :::
          githubWorkflowBuildPreamble.value.toList :::
          WorkflowStep.Run(
            List(s"${sbt.value} githubWorkflowCheck"),
            name = Some("Check that workflows are up to date")) ::
          githubWorkflowBuild.value.toList :::
          githubWorkflowBuildPostamble.value.toList :::
          uploadStepsOpt,
        sbtStepPreamble = githubWorkflowBuildSbtStepPreamble.value.toList,
        oses = githubWorkflowOSes.value.toList,
        scalas = githubWorkflowScalaVersions.value.toList,
        javas = githubWorkflowJavaVersions.value.toList,
        matrixFailFast = githubWorkflowBuildMatrixFailFast.value,
        matrixAdds = githubWorkflowBuildMatrixAdditions.value,
        matrixIncs = githubWorkflowBuildMatrixInclusions.value.toList,
        matrixExcs = githubWorkflowBuildMatrixExclusions.value.toList,
        runsOnExtraLabels = githubWorkflowBuildRunsOnExtraLabels.value.toList,
        timeoutMinutes = githubWorkflowBuildTimeoutMinutes.value
      )
    },
    githubWorkflowPublishJob := {
      WorkflowJob.Run(
        "publish",
        "Publish Artifacts",
        githubWorkflowJobSetup.value.toList :::
          githubWorkflowGeneratedDownloadSteps.value.toList :::
          githubWorkflowPublishPreamble.value.toList :::
          githubWorkflowPublish.value.toList :::
          githubWorkflowPublishPostamble.value.toList,
        cond = Some(publicationCond.value),
        oses = githubWorkflowOSes.value.toList.take(1),
        scalas = List.empty,
        sbtStepPreamble = List.empty,
        javas = List(githubWorkflowJavaVersions.value.head),
        needs = githubWorkflowPublishNeeds.value.toList,
        timeoutMinutes = githubWorkflowPublishTimeoutMinutes.value
      )
    },
    githubWorkflowCI := toWorkflow(
      name = "Continuous Integration",
      branches = githubWorkflowTargetBranches.value.toList,
      tags = githubWorkflowTargetTags.value.toList,
      paths = githubWorkflowTargetPaths.value,
      prEventTypes = githubWorkflowPREventTypes.value.toList,
      permissions = githubWorkflowPermissions.value,
      env = githubWorkflowEnv.value,
      concurrency = githubWorkflowConcurrency.value,
      jobs = githubWorkflowGeneratedCI.value.toList
    ),
    githubWorkflows := Map("ci" -> githubWorkflowCI.value) ++
      (if (githubWorkflowIncludeClean.value) Map("clean" -> cleanFlow)
       else Map.empty),
    githubWorkflowGeneratedCI := {
      val publishJobOpt: Seq[WorkflowJob] =
        Seq(githubWorkflowPublishJob.value).filter(_ =>
          githubWorkflowPublishTargetBranches.value.nonEmpty)

      Seq(githubWorkflowBuildJob.value) ++ publishJobOpt ++ githubWorkflowAddedJobs.value
    }
  )

  private val publicationCond = Def.setting {
    val publicationCondPre =
      githubWorkflowPublishTargetBranches
        .value
        .map(compileBranchPredicate("github.ref", _))
        .mkString("(", " || ", ")")

    val publicationCond = githubWorkflowPublishCond.value match {
      case Some(cond) => publicationCondPre + " && (" + cond + ")"
      case None => publicationCondPre
    }
    s"github.event_name != 'pull_request' && $publicationCond"
  }

  private val sbt = Def.setting {
    if (githubWorkflowUseSbtThinClient.value) {
      githubWorkflowSbtCommand.value + " --client"
    } else {
      githubWorkflowSbtCommand.value
    }
  }

  private val workflowsDirTask = Def.task {
    val githubDir = baseDirectory.value / ".github"
    val workflowsDir = githubDir / "workflows"

    if (!githubDir.exists()) {
      githubDir.mkdir()
    }

    if (!workflowsDir.exists()) {
      workflowsDir.mkdir()
    }

    workflowsDir
  }

  override def projectSettings = Seq(
    githubWorkflowArtifactUpload := publishArtifact.value,
    Global / internalTargetAggregation ++= {
      if (githubWorkflowArtifactUpload.value)
        Seq(target.value)
      else
        Seq()
    },
    githubWorkflowGenerate / aggregate := false,
    githubWorkflowCheck / aggregate := false,
    githubWorkflowGenerate := {
      val sbtV = sbt.value
      val workflowsDir = workflowsDirTask.value
      githubWorkflows
        .value
        .map {
          case (key, value) =>
            (workflowsDir / s"$key.yml") -> render(value, sbtV)
        }
        .foreach {
          case (file, contents) =>
            IO.write(file, contents)
        }
    },
    githubWorkflowCheck := {
      val sbtV = sbt.value
      val workflowsDir = workflowsDirTask.value
      val expectedFlows: Map[File, String] =
        githubWorkflows.value.map {
          case (key, value) =>
            (workflowsDir / s"$key.yml") -> render(value, sbtV)
        }

      val log = state.value.log

      def reportMismatch(file: File, expected: String, actual: String): Unit = {
        log.error(s"Expected:\n$expected")
        log.error(s"Actual:\n${diff(expected, actual)}")
        sys.error(
          s"${file.getName} does not contain contents that would have been generated by sbt-github-actions; try running githubWorkflowGenerate")
      }

      def compare(file: File, expected: String): Unit = {
        val actual = IO.read(file)
        if (expected != actual) {
          reportMismatch(file, expected, actual)
        }
      }

      expectedFlows.foreach(compare _ tupled)
    }
  )

  private[sbt] def expandMatrix(
      oses: List[String],
      scalas: List[String],
      javas: List[JavaSpec],
      matrixAdds: Map[String, List[String]],
      includes: List[MatrixInclude],
      excludes: List[MatrixExclude]
  ): List[List[String]] = {
    val keys =
      MatrixKeys.OS :: MatrixKeys.Scala :: MatrixKeys.Java :: matrixAdds.keys.toList.sorted

    val matrix =
      matrixAdds +
        (MatrixKeys.OS -> oses) +
        (MatrixKeys.Scala -> scalas) +
        (MatrixKeys.Java -> javas.map(_.render))

    // expand the matrix
    keys
      .filterNot(matrix.getOrElse(_, Nil).isEmpty)
      .foldLeft(List(List.empty[String])) { (cells, key) =>
        val values = matrix.getOrElse(key, Nil)
        cells.flatMap { cell => values.map(v => cell ::: v :: Nil) }
      }
      .filterNot { cell => // remove the excludes
        val job = keys.zip(cell).toMap
        excludes.exists { // there is an exclude that matches the current job
          case MatrixExclude(matching) => matching.toSet.subsetOf(job.toSet)
        }
      } ::: includes.map { // add the includes
      case MatrixInclude(matching, additions) =>
        // yoloing here, but let's wait for the bug report
        keys.map(matching) ::: additions.values.toList
    }
  }

  private[sbt] def diff(expected: String, actual: String): String = {
    val expectedLines = expected.split("\n", -1)
    val actualLines = actual.split("\n", -1)
    val (lines, _) =
      expectedLines.zipAll(actualLines, "", "").foldLeft((Vector.empty[String], false)) {
        case ((acc, foundDifference), (expectedLine, actualLine))
            if expectedLine == actualLine =>
          (acc :+ actualLine, foundDifference)
        case ((acc, false), ("", actualLine)) =>
          val previousLineLength = acc.lastOption.map(_.length).getOrElse(0)
          val padding = " " * previousLineLength
          val highlight = s"$padding^ (additional lines)"
          (acc :+ highlight :+ actualLine, true)
        case ((acc, false), (_, "")) =>
          val previousLineLength = acc.lastOption.map(_.length).getOrElse(0)
          val padding = " " * previousLineLength
          val highlight = s"$padding^ (missing lines)"
          (acc :+ highlight, true)
        case ((acc, false), (expectedLine, actualLine)) =>
          val sameCount =
            expectedLine.zip(actualLine).takeWhile { case (a, b) => a == b }.length
          val padding = " " * sameCount
          val highlight = s"$padding^ (different character)"
          (acc :+ actualLine :+ highlight, true)
        case ((acc, true), (_, "")) =>
          (acc, true)
        case ((acc, true), (_, actualLine)) =>
          (acc :+ actualLine, true)
      }
    lines.mkString("\n")
  }

  private val cleanFlow: Workflow =
    Workflow(on = List(WorkflowTrigger.Push()))
      .withName("Clean".some)
      .withJobs(
        WorkflowJob.Run(
          id = "delete-artifacts",
          name = "Delete Artifacts",
          env = Map("GITHUB_TOKEN" -> s"$${{ secrets.GITHUB_TOKEN }}"),
          scalas = List.empty,
          javas = List.empty,
          steps = WorkflowStep.Run(
            name = "Delete artifacts".some,
            commands =
              raw"""# Customize those three lines with your repository and credentials:
                   |REPO=$${GITHUB_API_URL}/repos/$${{ github.repository }}
                   |
                   |# A shortcut to call GitHub API.
                   |ghapi() { curl --silent --location --user _:$$GITHUB_TOKEN "$$@"; }
                   |
                   |# A temporary file which receives HTTP response headers.
                   |TMPFILE=/tmp/tmp.$$$$
                   |
                   |# An associative array, key: artifact name, value: number of artifacts of that name.
                   |declare -A ARTCOUNT
                   |
                   |# Process all artifacts on this repository, loop on returned "pages".
                   |URL=$$REPO/actions/artifacts
                   |while [[ -n "$$URL" ]]; do
                   |
                   |  # Get current page, get response headers in a temporary file.
                   |  JSON=$$(ghapi --dump-header $$TMPFILE "$$URL")
                   |
                   |  # Get URL of next page. Will be empty if we are at the last page.
                   |  URL=$$(grep '^Link:' "$$TMPFILE" | tr ',' '\n' | grep 'rel="next"' | head -1 | sed -e 's/.*<//' -e 's/>.*//')
                   |  rm -f $$TMPFILE
                   |
                   |  # Number of artifacts on this page:
                   |  COUNT=$$(( $$(jq <<<$$JSON -r '.artifacts | length') ))
                   |
                   |  # Loop on all artifacts on this page.
                   |  for ((i=0; $$i < $$COUNT; i++)); do
                   |
                   |    # Get name of artifact and count instances of this name.
                   |    name=$$(jq <<<$$JSON -r ".artifacts[$$i].name?")
                   |    ARTCOUNT[$$name]=$$(( $$(( $${ARTCOUNT[$$name]} )) + 1))
                   |
                   |    id=$$(jq <<<$$JSON -r ".artifacts[$$i].id?")
                   |    size=$$(( $$(jq <<<$$JSON -r ".artifacts[$$i].size_in_bytes?") ))
                   |    printf "Deleting '%s' #%d, %'d bytes\n" $$name $${ARTCOUNT[$$name]} $$size
                   |    ghapi -X DELETE $$REPO/actions/artifacts/$$id
                   |  done
                   |done""".stripMargin :: Nil
          ) :: Nil
        ) :: Nil
      )
}
