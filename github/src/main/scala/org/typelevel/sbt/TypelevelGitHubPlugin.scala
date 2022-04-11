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

import sbt._, Keys._

import scala.util.Try

object TypelevelGitHubPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = TypelevelKernelPlugin

  object autoImport {

    /**
     * Helper to create a `Developer` entry from a GitHub username.
     */
    def tlGitHubDev(user: String, fullName: String): Developer = {
      Developer(user, fullName, s"@$user", url(s"https://github.com/$user"))
    }

    lazy val tlGitHubRepo = settingKey[Option[String]]("The name of this repository on GitHub")
  }

  import autoImport._

  override def buildSettings = Seq(
    tlGitHubRepo := gitHubUserRepo.value.map(_._2),
    scmInfo := gitHubUserRepo.value.map {
      case (user, repo) =>
        gitHubScmInfo(user, repo)
    },
    homepage := homepage.value.orElse(scmInfo.value.map(_.browseUrl))
  )

  private def gitHubScmInfo(user: String, repo: String) =
    ScmInfo(
      url(s"https://github.com/$user/$repo"),
      s"scm:git:https://github.com/$user/$repo.git",
      s"scm:git:git@github.com:$user/$repo.git"
    )

  private[sbt] def gitHubUserRepo = Def.setting {
    import scala.sys.process._

    def fromRemote(remote: String) = {
      val identifier = """([^\/]+?)"""
      val GitHubHttps = s"https://github.com/$identifier/$identifier(?:\\.git)?".r
      val GitHubGit = s"git://github.com:$identifier/$identifier(?:\\.git)?".r
      val GitHubSsh = s"git@github.com:$identifier/$identifier(?:\\.git)?".r
      Try {
        List("git", "ls-remote", "--get-url", remote).!!.trim() match {
          case GitHubHttps(user, repo) => Some((user, repo))
          case GitHubGit(user, repo) => Some((user, repo))
          case GitHubSsh(user, repo) => Some((user, repo))
          case _ => None
        }
      }.toOption.flatten
    }

    // upstream if this is a fork, otherwise fallback to origin
    fromRemote("upstream").orElse(fromRemote("origin"))
  }

}
