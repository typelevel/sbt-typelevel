package org.typelevel.sbt

import sbt._, Keys._

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

}
