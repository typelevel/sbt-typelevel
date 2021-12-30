package org.typelevel.sbt

import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import MimaPlugin.autoImport._
import org.typelevel.sbt.kernel.GitHelper
import org.typelevel.sbt.kernel.V

object TypelevelMimaPlugin extends AutoPlugin {

  override def requires = MimaPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlVersionIntroduced =
      settingKey[Map[String, String]](
        "A map scalaBinaryVersion -> version e.g. Map('2.13' -> '1.5.2', '3' -> '1.7.1') used to indicate that a particular crossScalaVersions value was introduced in a given version (default: empty).")
  }

  import autoImport._

  override def projectSettings = Seq[Setting[_]](
    tlVersionIntroduced := Map.empty,
    mimaPreviousArtifacts := {
      require(
        versionScheme.value.contains("early-semver"),
        "Only early-semver versioning scheme supported.")
      if (publishArtifact.value) {
        val current = V(version.value)
          // Consider it as a real release, for purposes of compat-checking
          .map(_.copy(prerelease = None))
          .getOrElse(sys.error(s"Version must be semver format: ${version.value}"))
        val introduced = tlVersionIntroduced
          .value
          .get(scalaBinaryVersion.value)
          .map(v => V(v).getOrElse(sys.error(s"Version must be semver format: $v")))
        val previous = GitHelper
          .previousReleases()
          .filterNot(_.isPrerelease)
          .filter(v => introduced.forall(v >= _))
          .filter(current.mustBeBinCompatWith(_))
        previous
          .map(v =>
            projectID.value.withRevision(v.toString).withExplicitArtifacts(Vector.empty))
          .toSet
      } else {
        Set.empty
      }
    }
  )

}
