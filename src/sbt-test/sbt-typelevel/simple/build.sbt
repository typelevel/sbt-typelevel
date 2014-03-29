import org.typelevel.sbt._
import org.typelevel.sbt.Releasing.Stages


organization := "org.example"

name := "simple"

publishTo := Some(Resolver.file("repo", new File("./repo")))

resolvers := Seq("r-repo" at s"file://${System.getProperty("user.dir")}/repo")

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.9.3", "2.10.3")


TypelevelPlugin.typelevelDefaultSettings

TypelevelKeys.signArtifacts := false

val testVersions = List[sbtrelease.ReleaseStep]({ st: State =>
  st.put(Releasing.versions, ((Version.Relative(4, Version.Final), Version.Relative(5, Version.Snapshot))))
})

ReleaseKeys.releaseProcess :=
  Stages.checks ++
  testVersions ++
  Stages.pre ++
  Stages.publish ++
  Stages.post
