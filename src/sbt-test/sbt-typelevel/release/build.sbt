import org.typelevel.sbt.{Version => TVersion, _}
import org.typelevel.sbt.Releasing.Stages


organization := "org.example"

name := "release"

resolvers := Seq("r-repo" at s"file://${System.getProperty("user.dir")}/repo")

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.9.3", "2.10.3")


typelevelDefaultSettings

TypelevelKeys.stability := Stability.Stable

TypelevelKeys.signArtifacts := false

TypelevelKeys.githubDevs += Developer("Lars Hupel", "larsrh")

TypelevelKeys.githubProject := ("typelevel", "sbt-typelevel")

val testVersions = List[sbtrelease.ReleaseStep]({ st: State =>
  st.put(Releasing.versions, ((TVersion.Relative(4, TVersion.Final), TVersion.Relative(5, TVersion.Snapshot))))
})

ReleaseKeys.releaseProcess :=
  Stages.checks ++
  testVersions ++
  Stages.pre ++
  Stages.publish ++
  Stages.post

publishTo := Some(Resolver.file("repo", new File("./repo")))
