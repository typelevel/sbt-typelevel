
import org.typelevel.sbt._
import org.typelevel.sbt.ReleaseSeries._
import org.typelevel.sbt.Version._

TypelevelKeys.currentSeries in ThisBuild := ReleaseSeries(2,3,Stable)

TypelevelKeys.currentVersion in ThisBuild := Relative(4,Snapshot)
