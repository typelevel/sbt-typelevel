package org.typelevel.sbt

import scala.util.control.Exception._

object Version {
  object Relative {
    private val SnapshotR = """(\d+)-SNAPSHOT""".r
    private val MilestoneR = """(\d+)-M(\d+)""".r
    private val RCR = """(\d+)-RC(\d+)""".r
    private val FinalR = """(\d+)""".r

    def fromString(str: String): Option[Relative] = allCatch opt {
      val (value, suffix) =
        str match {
          case SnapshotR(value) => (value, Snapshot)
          case MilestoneR(value, count) => (value, Milestone(count.toInt))
          case RCR(value, count) => (value, RC(count.toInt))
          case FinalR(value) => (value, Final)
        }
      Relative(value.toInt, suffix)
    }
  }

  private val VersionR = """(\d+)\.(\d+)\.(.*)""".r

  def fromString(str: String): Option[Version] = {
    val parsed = allCatch opt {
      str match {
        case VersionR(major, minor, rest) => (ReleaseSeries(major.toInt, minor.toInt), rest)
      }
    }
    
    for {
      (series, rest) <- parsed
      relative <- Relative.fromString(rest)
    } yield Version(series, relative)
  }

  case class Relative(value: Int, suffix: Suffix) {
    def id = suffix.id.fold(value.toString)(s => s"$value-$s")

    def isStable: Boolean =
      value > 0 || suffix == Final

    override def toString = s"Relative($value, $suffix)"
  }

  sealed trait Suffix {
    def id = this match {
      case Snapshot => Some("SNAPSHOT")
      case Milestone(c) => Some(s"M$c")
      case RC(c) => Some(s"RC$c")
      case Final => None
    }
  }

  case object Snapshot extends Suffix
  case class Milestone(count: Int) extends Suffix
  case class RC(count: Int) extends Suffix
  case object Final extends Suffix
}

case class Version(series: ReleaseSeries, relative: Version.Relative) {
  def id = s"${series.id}.${relative.id}"

  def compatible(that: Version) =
    (this.series == that.series) && this.relative.isStable && that.relative.isStable
      
}
