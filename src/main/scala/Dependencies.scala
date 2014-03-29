package org.typelevel.sbt

import sbt._
import sbt.impl.GroupArtifactID

import net.virtualvoid.sbt.graph.IvyGraphMLDependencies.{Module, ModuleId}

object Dependencies {

  object known {
    val scala = Dependency("org.scala-lang", "scala-library", ReleaseSeries(2, 10))

    private def scalaz(module: String) = Dependency("org.scalaz", s"scalaz-$module", ReleaseSeries(7, 0))
    val scalazCore = scalaz("core")
    val scalazScalacheckBinding = scalaz("scalacheck-binding")

    val scodec = Dependency("org.typelevel", "scodec-core", ReleaseSeries(1, 0))
    val scalacheck = Dependency("org.scalacheck", "scalacheck", ReleaseSeries(1, 10))

    val all = List(scala, scalazCore, scalazScalacheckBinding, scodec, scalacheck)
  }

  def findFromIDs(all: List[Dependency], groupID: String, rawArtifactID: String): Option[Dependency] = {
    val ScalaSuffix = """(.*)_[.0-9]*""".r
    val artifactID = rawArtifactID match {
      case ScalaSuffix(artifactID) => artifactID
      case _ => rawArtifactID
    }
    all.find(d => d.groupID == groupID && d.artifactID == artifactID)
  }

  def check(all: List[Dependency], logger: Logger, modules: Seq[Module]) = modules.foreach {
    case Module(ModuleId(groupID, artifactID, olderV), _, _, Some(newerV), _) =>
      val msg = s"Version mismatch in $groupID#$artifactID: $olderV and $newerV are different"

      findFromIDs(all, groupID, artifactID) match {
        case Some(dep) =>
          (Version.fromString(olderV), Version.fromString(newerV)) match {
            case (Some(o), Some(n)) =>
              if (dep.minCompatible <= o.series && o.compatible(n))
                logger.info(s"Compatible versions in $groupID#$artifactID: $olderV and $newerV")
              else
                logger.error(msg)
            case _ =>
              logger.error(msg + ", and version numbers cannot be parsed")
          }
        case None =>
          logger.error(msg + ", and no dependency information available")
      }
    case _ =>
  }

}

case class Dependency(groupID: String, artifactID: String, minCompatible: ReleaseSeries)
