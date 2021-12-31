package org.typelevel.sbt.kernel

import scala.util.Try

import scala.sys.process._

object GitHelper {

  /**
   * @param fromHead
   *   if `true`, only tags reachable from HEAD's history. If `false`, all tags in the repo.
   */
  def previousReleases(fromHead: Boolean = false): List[V] = {
    Try {
      val merged = if (fromHead) " --merged" else ""
      s"git tag --list$merged".!!.split("\n").toList.map(_.trim).collect {
        case V.Tag(version) => version
      }
    }.getOrElse(List.empty).sorted.reverse
  }

  def getTagOrHash(tags: Seq[String], hash: Option[String]): Option[String] =
    tags.collect { case v @ V.Tag(_) => v }.headOption.orElse(hash)

}
