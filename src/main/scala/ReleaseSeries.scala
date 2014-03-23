package org.typelevel.sbt

object ReleaseSeries {
  sealed abstract class Stability(name: String)

  case object Stable extends Stability("Stable")
  case object Development extends Stability("Development")
}

case class ReleaseSeries(major: Int, minor: Int, stability: ReleaseSeries.Stability) {
  def id = s"$major.$minor"
}
