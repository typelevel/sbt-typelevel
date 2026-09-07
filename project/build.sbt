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

libraryDependencies ++= Seq(
  // TODO: Zainab - Coursier doesn't seem to be published for Scala 3. Check whether this dependency is needed.
  // "io.get-coursier" %% "coursier" % "2.1.24"
)
