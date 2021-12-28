package org.typelevel.sbt

import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import MimaPlugin.autoImport._
import org.typelevel.sbt.kernel.V

import scala.util.Try

object TypelevelMimaPlugin extends AutoPlugin {

  override def requires = MimaPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlVersionIntroduced =
      settingKey[Option[String]]("The version in which this module was introduced.")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    tlVersionIntroduced := None,
    mimaPreviousArtifacts := {
      require(
        versionScheme.value.contains("early-semver"),
        "Only early-semver versioning scheme supported.")
      if (publishArtifact.value) {
        val current = V(version.value)
          .getOrElse(sys.error(s"Version must be semver format: ${version.value}"))
        val introduced = tlVersionIntroduced
          .value
          .map(v => V(v).getOrElse(sys.error(s"Version must be semver format: $v")))
        val previous = previousReleases()
          .filterNot(_.isPrerelease)
          .filter(v => introduced.forall(v >= _))
          .filter(current.mustBeBinCompatWith(_))
        previous.map(v => projectID.value.withRevision(v.toString)).toSet
      } else {
        Set.empty
      }
    }
  )

  def previousReleases(): List[V] = {
    import scala.sys.process._
    Try {
      "git tag --list"
        .!!
        .split("\n")
        .toList
        .map(_.trim)
        .collect { case V.Tag(version) => version }
    }.getOrElse(List.empty)
  }

}
