package org.typelevel.sbt

import sbt._

object TypelevelCiReleasePlugin extends AutoPlugin {

  override def requires =
    TypelevelSonatypeCiReleasePlugin && TypelevelCiSigningPlugin && TypelevelPlugin

  override def trigger = noTrigger

}
