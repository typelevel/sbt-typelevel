package org.typelevel.sbt

import scala.util.control.Exception._

object ReleaseSeries {
  private val SeriesR = """(\d+)\.(\d+)""".r

  def fromString(str: String): Option[ReleaseSeries] =
    allCatch opt {
      str match {
        case SeriesR(major, minor) => ReleaseSeries(major.toInt, minor.toInt)
      }
    }
}

case class ReleaseSeries(major: Int, minor: Int) {
  def id = s"$major.$minor"

  def <=(that: ReleaseSeries) =
    this.major < that.major || (this.major == that.major && this.minor <= that.minor)

  override def toString = s"ReleaseSeries($major, $minor)"
}
