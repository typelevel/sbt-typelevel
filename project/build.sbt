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
  Seq(moduleDir / "scala", moduleDir / "scala-2.12")
}

Compile / unmanagedResourceDirectories ++= modules.map { module =>
  baseDirectory.value.getParentFile / module / "src" / "main" / "resources"
}

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "2.1.24"
)
