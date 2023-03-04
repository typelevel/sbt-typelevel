name := "sbt-typelevel"

ThisBuild / tlBaseVersion := "0.5"
ThisBuild / tlSitePublishBranch := Some("series/0.4")
ThisBuild / crossScalaVersions := Seq("2.12.17")
ThisBuild / developers ++= List(
  tlGitHubDev("armanbilge", "Arman Bilge"),
  tlGitHubDev("rossabaker", "Ross A. Baker"),
  tlGitHubDev("ChristopherDavenport", "Christopher Davenport"),
  tlGitHubDev("djspiewak", "Daniel Spiewak")
)
ThisBuild / startYear := Some(2022)

ThisBuild / githubWorkflowJavaVersions ++=
  Seq(JavaSpec.temurin("17"), JavaSpec(JavaSpec.Distribution.GraalVM("latest"), "17"))

ThisBuild / githubWorkflowPublishTimeoutMinutes := Some(45)

ThisBuild / mergifyStewardConfig ~= {
  _.map(_.copy(mergeMinors = true, author = "typelevel-steward[bot]"))
}
ThisBuild / mergifySuccessConditions += MergifyCondition.Custom("#approved-reviews-by>=1")
ThisBuild / mergifyLabelPaths += { "docs" -> file("docs") }
ThisBuild / mergifyLabelPaths ~= { _ - "unidoc" }
ThisBuild / mergifyPrRules += MergifyPrRule(
  "assign scala-steward's PRs for review",
  List(MergifyCondition.Custom("author=typelevel-steward[bot]")),
  List(
    MergifyAction.RequestReviews.fromUsers("armanbilge")
  )
)

ThisBuild / scalafixDependencies ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0"
)

val MunitVersion = "0.7.29"

lazy val `sbt-typelevel` = tlCrossRootProject.aggregate(
  kernel,
  noPublish,
  settings,
  github,
  githubActions,
  mergify,
  versioning,
  mima,
  sonatype,
  scalafix,
  ciSigning,
  sonatypeCiRelease,
  ci,
  core,
  ciRelease,
  site,
  unidoc,
  docs
)

lazy val kernel = project
  .in(file("kernel"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-kernel",
    libraryDependencies += "org.scalameta" %% "munit" % MunitVersion % Test
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

lazy val mergify = project
  .in(file("mergify"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-mergify",
    tlVersionIntroduced := Map("2.12" -> "0.4.6")
  )
  .dependsOn(githubActions)

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

lazy val scalafix = project
  .in(file("scalafix"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-typelevel-scalafix",
    tlVersionIntroduced := Map("2.12" -> "0.4.10")
  )

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
  .dependsOn(kernel, github, githubActions, noPublish)

lazy val unidoc = project
  .in(file("unidoc"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "sbt-typelevel-docs"
  )

lazy val docs = project
  .in(file("mdocs"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    laikaConfig ~= { _.withRawContent },
    tlSiteApiPackage := Some("org.typelevel.sbt"),
    tlSiteRelatedProjects := Seq(
      "typelevel.g8" -> url("https://github.com/typelevel/typelevel.g8"),
      "sbt" -> url("https://www.scala-sbt.org/"),
      "sbt-crossproject" -> url("https://github.com/portable-scala/sbt-crossproject"),
      "mima" -> url("https://github.com/lightbend/mima"),
      "mdoc" -> url("https://scalameta.org/mdoc/"),
      "Laika" -> url("https://planet42.github.io/Laika/"),
      "sbt-unidoc" -> url("https://github.com/sbt/sbt-unidoc")
    ),
    tlSiteIsTypelevelProject := true,
    mdocVariables ++= {
      import coursier.complete.Complete
      import java.time._
      import scala.concurrent._
      import scala.concurrent.duration._
      import scala.concurrent.ExecutionContext.Implicits._

      val startYear = YearMonth.now().getYear.toString

      val sjsVersionFuture =
        Complete().withInput(s"org.scala-js:scalajs-library_2.13:").complete().future()
      val sjsVersion =
        try {
          Await.result(sjsVersionFuture, 5.seconds)._2.last
        } catch {
          case ex: TimeoutException => scalaJSVersion // not the latest but better than nothing
        }

      Map(
        "START_YEAR" -> startYear,
        "LATEST_SJS_VERSION" -> sjsVersion
      )
    }
  )
