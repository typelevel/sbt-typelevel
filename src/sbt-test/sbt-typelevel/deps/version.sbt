
import org.typelevel.sbt._
import org.typelevel.sbt.ReleaseSeries._
import org.typelevel.sbt.Version._

TypelevelKeys.series in ThisBuild := ReleaseSeries(2,3)

TypelevelKeys.relativeVersion in ThisBuild := Relative(4,Snapshot)
