import org.typelevel.sbt._
import org.typelevel.sbt.Releasing.Stages


Typelevel.typelevelSettings

val testVersions = List[sbtrelease.ReleaseStep]({ st: State =>
  st.put(Releasing.versions, ((Version.Relative(4, Version.Final), Version.Relative(5, Version.Snapshot))))
})

ReleaseKeys.releaseProcess :=
  Stages.checks ++
  testVersions ++
  Stages.pre ++
  Stages.post
