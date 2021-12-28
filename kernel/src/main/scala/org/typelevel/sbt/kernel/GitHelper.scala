package org.typelevel.sbt.kernel

import scala.util.Try

import scala.sys.process._

object GitHelper {

  def previousReleases(): List[V] = {
    Try {
      "git tag --list".!!.split("\n").toList.map(_.trim).collect {
        case V.Tag(version) => version
      }
    }.getOrElse(List.empty).sorted.reverse
  }

}
