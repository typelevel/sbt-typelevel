package org.typelevel.sbt

import sbt._
import sbt.Keys._

import org.typelevel.sbt.TypelevelPlugin.TypelevelKeys

import net.virtualvoid.sbt.graph.{Plugin => GraphPlugin}
import net.virtualvoid.sbt.graph.IvyGraphMLDependencies.{Module, ModuleId}

object Dependencies {

  object known {
    val scala = Dependency("org.scala-lang", "scala-library", ReleaseSeries(2, 10), false)

    private def scalaz(module: String) = Dependency("org.scalaz", s"scalaz-$module", ReleaseSeries(7, 0))
    val scalazCore = scalaz("core")
    val scalazScalacheckBinding = scalaz("scalacheck-binding")

    val scodec = Dependency("org.typelevel", "scodec-core", ReleaseSeries(1, 0))
    val scalacheck = Dependency("org.scalacheck", "scalacheck", ReleaseSeries(1, 10))

    val all = List(scala, scalazCore, scalazScalacheckBinding, scodec, scalacheck)
  }

  def findFromIDs(all: List[Dependency], groupID: String, rawArtifactID: String): Option[Dependency] =
    all.filter(_.matches(groupID, rawArtifactID)) match {
      case head :: Nil => Some(head)
      case Nil => None
      case _ => sys.error(s"Internal error: found more than one match for $groupID:$rawArtifactID; check your `knownDependencies` setting")
    }

  def check(all: List[Dependency], logger: Logger, modules: Seq[Module]) = {
    val notices = modules.collect {
      case Module(ModuleId(groupID, artifactID, olderV), _, _, Some(newerV), _) =>
        val msg = s"Version mismatch in $groupID#$artifactID: $olderV and $newerV are different"

        findFromIDs(all, groupID, artifactID) match {
          case Some(dep) =>
            (Version.fromString(olderV), Version.fromString(newerV)) match {
              case (Some(o), Some(n)) =>
                if (dep.minCompatible <= o.series && o.compatible(n))
                  Right(s"Compatible versions in $groupID#$artifactID: $olderV and $newerV")
                else
                  Left(msg)
              case _ =>
                Left("$msg, and version numbers cannot be parsed")
            }
          case None =>
            Left(s"$msg, and no dependency information available")
        }
    }

    notices.foreach(_.fold(logger.error(_), logger.info(_)))

    val count = notices.filter(_.isLeft).length
    if (count > 0)
      sys.error(s"Dependency check failed, found $count version mismatch(es)")
  }

  def checkSettings(config: Configuration) = inConfig(config)(seq(
    TypelevelKeys.checkDependencies :=
      check(
        TypelevelKeys.knownDependencies.value.toList,
        streams.value.log,
        GraphPlugin.moduleGraph.value.nodes
      )
  ))

}

case class Dependency(groupID: String, artifactID: String, minCompatible: ReleaseSeries, scalaSuffix: Boolean = true) {

  def matches(thatGroupID: String, thatArtifactID: String): Boolean = {
    val ScalaSuffix = """(.*)_[.0-9]*""".r
    if (groupID != thatGroupID)
      false
    else
      thatArtifactID match {
        case ScalaSuffix(`artifactID`) if scalaSuffix => true
        case `artifactID` if !scalaSuffix => true
        case _ => false
      }
  }

}
