package org.typelevel.sbt

import sbt._, Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import com.typesafe.sbt.SbtGit.git
import org.typelevel.sbt.kernel.GitHelper

object TypelevelScalaJSGitHubPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = ScalaJSPlugin && TypelevelKernelPlugin

  import TypelevelKernelPlugin.autoImport._

  override def projectSettings = Seq(
    scalacOptions ++= {
      val flag = if (tlIsScala3.value) "-scalajs-mapSourceURI:" else "-P:scalajs:mapSourceURI:"

      val tagOrHash =
        GitHelper.getTagOrHash(git.gitCurrentTags.value, git.gitHeadCommit.value)

      val l = (LocalRootProject / baseDirectory).value.toURI.toString

      tagOrHash.flatMap { v =>
        scmInfo.value.map { info =>
          val g =
            s"${info.browseUrl.toString.replace("github.com", "raw.githubusercontent.com")}/$v/"
          s"$flag$l->$g"
        }
      }
    }
  )
}
