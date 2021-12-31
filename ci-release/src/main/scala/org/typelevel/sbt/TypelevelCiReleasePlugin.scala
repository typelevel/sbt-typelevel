package org.typelevel.sbt

import sbt._

object TypelevelCiReleasePlugin extends AutoPlugin {

  override def requires =
    TypelevelVersioningPlugin &&
      TypelevelMimaPlugin &&
      TypelevelCiPlugin &&
      TypelevelSonatypeCiReleasePlugin &&
      TypelevelCiSigningPlugin

  override def trigger = noTrigger

}
