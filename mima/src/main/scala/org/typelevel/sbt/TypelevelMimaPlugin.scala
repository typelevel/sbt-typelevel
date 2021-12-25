package org.typelevel.sbt

import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import MimaPlugin.autoImport._

import scala.util.Try

object TypelevelMimaPlugin extends AutoPlugin {

  override def requires = MimaPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlVersionIntroduced =
      settingKey[Option[String]]("The version in which this module was introduced.")
  }

  override def buildSettings: Seq[Setting[_]] = Seq(
    mimaPreviousArtifacts := {
      if (publishArtifact.value) {
        Set.empty
      } else {
        Set.empty
      }
    }
  )

  val ReleaseTag = """^v((?:\d+\.){2}\d+)$""".r

  def previousReleases(): Seq[String] = {
    import scala.sys.process._
    Try("git tag --list".!!.split("\n").toList.map(_.trim).collect {
      case version @ ReleaseTag(_) => version
    }).getOrElse(Seq.empty)
  }

}
