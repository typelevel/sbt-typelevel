import org.typelevel.sbt.{Version => TVersion, _}
import org.typelevel.sbt.Releasing.Stages


organization := "org.example"

name := "release"

resolvers := Seq("r-repo" at s"file://${System.getProperty("user.dir")}/repo")

scalaVersion := "2.10.3"


typelevelDefaultSettings

TypelevelKeys.signArtifacts := false

val dummyVersions = List[sbtrelease.ReleaseStep]({ st: State =>
  val releaseS = ReleaseSeries(2, 3)
  val releaseV = TVersion.Relative(0, TVersion.Final)
  val nextS = ReleaseSeries(2, 4)
  val nextV = TVersion.Relative(0, TVersion.Snapshot)
  st.put(Releasing.versions, (TVersion(releaseS, releaseV), TVersion(nextS, nextV)))
})

ReleaseKeys.releaseProcess :=
  Stages.checks ++
  dummyVersions ++
  Stages.pre ++
  Stages.publish ++
  Stages.post

publishTo := Some(Resolver.file("repo", new File("./repo")))
