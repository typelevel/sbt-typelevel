
import org.typelevel.sbt.ReleaseSeries
import org.typelevel.sbt.Version._

TypelevelKeys.series in ThisBuild := ReleaseSeries(2,3)

TypelevelKeys.relativeVersion in ThisBuild := Relative(5,Snapshot)

TypelevelKeys.lastRelease in ThisBuild := Relative(4,Final)
