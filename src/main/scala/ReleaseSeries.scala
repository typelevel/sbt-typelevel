package org.typelevel.sbt

case class ReleaseSeries(major: Int, minor: Int) {
  def id = s"$major.$minor"

  def <=(that: ReleaseSeries) =
    this.major < that.major || (this.major == that.major && this.minor <= that.minor)
}
