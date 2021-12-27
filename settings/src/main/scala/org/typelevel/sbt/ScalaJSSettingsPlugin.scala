package org.typelevel.sbt

import sbt._, Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin

object ScalaJSSettingsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = ScalaJSPlugin && TypelevelSettingsPlugin

  override def projectSettings = Seq(
  )
}
