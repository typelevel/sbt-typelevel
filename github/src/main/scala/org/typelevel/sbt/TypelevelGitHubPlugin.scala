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
  }

  override def buildSettings = Seq(
    scmInfo := getScmInfo(),
    homepage := homepage.value.orElse(scmInfo.value.map(_.browseUrl))
  )

  def getScmInfo(): Option[ScmInfo] = {
    import scala.sys.process._

    def gitHubScmInfo(user: String, repo: String) =
      ScmInfo(
        url(s"https://github.com/$user/$repo"),
        s"scm:git:https://github.com/$user/$repo.git",
        s"scm:git:git@github.com:$user/$repo.git"
      )

    val identifier = """([^\/]+?)"""
    val GitHubHttps = s"https://github.com/$identifier/$identifier(?:\\.git)?".r
    val GitHubGit = s"git://github.com:$identifier/$identifier(?:\\.git)?".r
    val GitHubSsh = s"git@github.com:$identifier/$identifier(?:\\.git)?".r
    Try {
      val remote = List("git", "ls-remote", "--get-url", "origin").!!.trim()
      remote match {
        case GitHubHttps(user, repo) => Some(gitHubScmInfo(user, repo))
        case GitHubGit(user, repo) => Some(gitHubScmInfo(user, repo))
        case GitHubSsh(user, repo) => Some(gitHubScmInfo(user, repo))
        case _ => None
      }
    }.toOption.flatten
  }

}
