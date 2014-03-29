package org.typelevel.sbt

case class ReleaseSeries(major: Int, minor: Int) {
  def id = s"$major.$minor"
}
