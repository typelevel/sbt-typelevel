val modules = List(
  "ci",
  "ci-release",
  "ci-signing",
  "core",
  "github",
  "github-actions",
  "kernel",
  "mergify",
  "mima",
  "no-publish",
  "scalafix",
  "settings",
  "site",
  "sonatype",
  "sonatype-ci-release",
  "versioning"
)

Compile / unmanagedSourceDirectories ++= modules.flatMap { module =>
  val moduleDir = baseDirectory.value.getParentFile / module / "src" / "main"
  Seq(moduleDir / "scala", moduleDir / "scala-3")
}

Compile / unmanagedResourceDirectories ++= modules.map { module =>
  baseDirectory.value.getParentFile / module / "src" / "main" / "resources"
}

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.7.0")
