package org.typelevel.sbt

import scala.util.control.Exception._

object Version {
  object Relative {
    val SnapshotR = """(\d+)-SNAPSHOT""".r
    val MilestoneR = """(\d+)-M(\d+)""".r
    val RCR = """(\d+)-RC(\d+)""".r
    val FinalR = """(\d+)""".r

    def fromString(str: String) = catching(classOf[NumberFormatException]) opt {
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

  case class Relative(value: Int, suffix: Suffix) {
    def id = suffix.id.fold(value.toString)(s => s"$value-$s")
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
}
