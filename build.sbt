name := "sbt-typelevel"

import org.typelevel.sbt.gha.{PermissionScope, PermissionValue, Permissions}

ThisBuild / tlBaseVersion := "0.7"
ThisBuild / crossScalaVersions := Seq("2.12.20")
ThisBuild / developers ++= List(
  tlGitHubDev("armanbilge", "Arman Bilge"),
  tlGitHubDev("rossabaker", "Ross A. Baker"),
  tlGitHubDev("ChristopherDavenport", "Christopher Davenport"),
  tlGitHubDev("djspiewak", "Daniel Spiewak")
)
ThisBuild / startYear := Some(2022)

val temurin8 = JavaSpec.temurin("8")
val temurin17 = JavaSpec.temurin("17")

ThisBuild / githubWorkflowJavaVersions ++= Seq(
  JavaSpec.temurin("11"),
  temurin17,
  JavaSpec(JavaSpec.Distribution.GraalVM("22.3.2"), "11"),
  JavaSpec.graalvm("21"),
  JavaSpec.corretto("17"),
  JavaSpec.semeru("17")
)

val macOS = "macos-latest"

ThisBuild / githubWorkflowOSes ++= Seq(macOS, "windows-latest")

ThisBuild / githubWorkflowBuildMatrixExclusions ++= {
  val exclusions = for {
    java <- githubWorkflowJavaVersions.value.tail
    os <- githubWorkflowOSes.value.tail
    if !(java == temurin17 && os == macOS) // keep this one
  } yield MatrixExclude(Map("java" -> java.render, "os" -> os))

  exclusions :+ MatrixExclude(Map("java" -> temurin8.render, "os" -> macOS))
}

ThisBuild / githubWorkflowPublishTimeoutMinutes := Some(45)
ThisBuild / githubWorkflowPublishNeeds += "validate-steward"

ThisBuild / githubWorkflowBuild += WorkflowStep.Run(
  List("pwd"),
  workingDirectory = Some("project")
)

ThisBuild / mergifyStewardConfig ~= {
  _.map(_.withMergeMinors(true).withAuthor("typelevel-steward[bot]"))
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
ThisBuild / mergifyRequiredJobs ++= Seq("validate-steward", "site")

ThisBuild / githubWorkflowPermissions := Some(Permissions.Specify.defaultPermissive)

val MunitVersion = "1.1.1"

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
  .dependsOn(noPublish)

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
    tlSiteHelium ~= {
      import laika.helium.config._
      _.site.mainNavigation(appendLinks = Seq(
        ThemeNavigationSection(
          "Related Projects",
          TextLink.external("https://github.com/typelevel/typelevel.g8", "typelevel.g8"),
          TextLink.external("https://www.scala-sbt.org/", "sbt"),
          TextLink
            .external("https://github.com/portable-scala/sbt-crossproject", "sbt-crossproject"),
          TextLink.external("https://github.com/lightbend/mima", "MiMa"),
          TextLink.external("https://scalameta.org/mdoc/", "mdoc"),
          TextLink.external("https://typelevel.org/Laika/", "Laika"),
          TextLink.external("https://github.com/sbt/sbt-unidoc", "sbt-unidoc"),
          TextLink.external(
            "https://github.com/scalacenter/sbt-dependency-submission",
            "sbt-dependency-submission"),
          TextLink
            .external("https://github.com/scala-steward-org/scala-steward", "Scala Steward")
        )
      ))
    },
    mdocVariables ++= {
      import coursier.complete.Complete
      import java.time._
      import scala.concurrent._
      import scala.concurrent.duration._
      import scala.concurrent.ExecutionContext.Implicits._

      val startYear = YearMonth.now().getYear.toString

      def getLatestVersion(dep: String) = {
        import scala.util.Try
        val fut = Complete().withInput(dep).complete().future()
        Try(Await.result(fut, 5.seconds)._2.last).toOption
      }

      val latestScalaJSVersion =
        getLatestVersion(s"org.scala-js:scalajs-library_2.13:").getOrElse(scalaJSVersion)
      val latestNativeVersion =
        getLatestVersion(s"org.scala-native:nativelib_native0.4_3:").getOrElse(nativeVersion)

      Map(
        "START_YEAR" -> startYear,
        "LATEST_SJS_VERSION" -> latestScalaJSVersion,
        "LATEST_NATIVE_VERSION" -> latestNativeVersion
      )
    }
  )
