name := "sbt-typelevel"

ThisBuild / tlBaseVersion := "0.4"
ThisBuild / tlCiReleaseBranches := Seq("series/0.4")
ThisBuild / crossScalaVersions := Seq("2.12.15")
ThisBuild / developers := List(
  tlGitHubDev("armanbilge", "Arman Bilge"),
  tlGitHubDev("rossabaker", "Ross A. Baker"),
  tlGitHubDev("ChristopherDavenport", "Christopher Davenport"),
  tlGitHubDev("djspiewak", "Daniel Spiewak")
)

lazy val root = tlCrossRootProject.aggregate(
  kernel,
  noPublish,
  settings,
  github,
  githubActions,
  versioning,
  mima,
  sonatype,
  ciSigning,
  sonatypeCiRelease,
  ci,
  core,
  ciRelease,
  site,
  docs
)

lazy val kernel = project
  .in(file("kernel"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-kernel"
  )

lazy val noPublish = project
  .in(file("no-publish"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-no-publish"
  )

lazy val settings = project
  .in(file("settings"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-settings"
  )
  .dependsOn(kernel)

lazy val github = project
  .in(file("github"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-github"
  )
  .dependsOn(kernel)

lazy val githubActions = project
  .in(file("github-actions"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-github-actions"
  )

lazy val versioning = project
  .in(file("versioning"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-versioning"
  )
  .dependsOn(kernel)

lazy val mima = project
  .in(file("mima"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-mima"
  )
  .dependsOn(kernel)

lazy val sonatype = project
  .in(file("sonatype"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-sonatype"
  )
  .dependsOn(kernel)

lazy val ciSigning = project
  .in(file("ci-signing"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci-signing"
  )
  .dependsOn(githubActions)

lazy val sonatypeCiRelease = project
  .in(file("sonatype-ci-release"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-sonatype-ci-release"
  )
  .dependsOn(sonatype, githubActions)

lazy val ci = project
  .in(file("ci"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci"
  )
  .dependsOn(noPublish, kernel, githubActions)

lazy val ciRelease = project
  .in(file("ci-release"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-ci-release"
  )
  .dependsOn(
    noPublish,
    github,
    versioning,
    mima,
    ci,
    sonatypeCiRelease,
    ciSigning
  )

lazy val core = project
  .in(file("core"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel"
  )
  .dependsOn(
    ciRelease,
    settings
  )

lazy val site = project
  .in(file("site"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-site"
  )
  .dependsOn(kernel, githubActions, noPublish)

lazy val unidoc = project
  .in(file("unidoc"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    // Doesn't conflict w/ core artifact since this one won't have the sbt cross version suffix
    // javadoc.io doesn't recognize sbt artifacts, so we must publish like this
    name := "sbt-typelevel"
  )

lazy val docs = project
  .in(file("mdocs"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(laikaConfig ~= { _.withRawContent })
