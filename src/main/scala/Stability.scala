package org.typelevel.sbt

object Stability {
  case object Stable extends Stability
  case object Development extends Stability
}

sealed abstract class Stability
