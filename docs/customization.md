# Customization

The complete list of plugins, settings, and utilities is given below. The **sbt-typelevel-ci-release** and **sbt-typelevel** super-plugins automatically load most of them. The diagram at the bottom of the page shows their inter-dependencies.

Instead of using the super-plugins, for finer-grained control you can always add plugins individually to your project and even build your own custom super-plugin.

## Modules

### sbt-typelevel-no-publish
`NoPublishPlugin`

### sbt-typelevel-kernel
`TypelevelKernelPlugin`

- `tlIsScala3` (setting): `true`, if `scalaVersion` is 3.x.
- `tlCommandAliases` (setting): Command aliases defined for this build.
- `tlReleaseLocal` (command): Alias for `+publishLocal`.

### sbt-typelevel-versioning
`TypelevelVersioningPlugin`: Establishes a git-based, early semantic versioning scheme.

- `tlBaseVersion` (setting): The series your project is in, e.g., 0.2, 3.5.
- `tlUntaggedAreSnapshots` (setting): If true, an untagged commit is given a snapshot version, e.g. `0.4.1-17-00218f9-SNAPSHOT`. If false, it is given a release version, e.g. `0.4.1-17-00218f9` (default: true).

### sbt-typelevel-mima
`TypelevelMimaPlugin`: Determines previous MiMa artifacts via your `version` setting and git tags.

- `tlVersionIntroduced` (setting): A map `scalaBinaryVersion -> version` e.g. `Map("2.13" -> "1.5.2", "3" -> "1.7.1")` used to indicate that a particular `crossScalaVersions` value was introduced in a given version (default: empty).
- `tlMimaPreviousVersions` (setting): A set of previous versions to compare binary-compatibility against.

### sbt-typelevel-sonatype
`TypelevelSonatypePlugin`: Sets up publishing to Sonatype/Maven.

- `tlSonatypeUseLegacyHost` (setting): Publish to `oss.sonatype.org` instead of `s01.oss.sonatype.org` (default: false).
- `tlRelease` (command): Check binary-compatibility and `+publish` to Sonatype.

`TypelevelUnidocPlugin`: Sets up publishing a Scaladoc-only artifact to Sonatype/Maven.

### sbt-typelevel-settings
`TypelevelSettingsPlugin`: Good and/or opinionated defaults for scalac settings etc., inspired by sbt-tpolecat.

- `tlFatalWarnings` (setting): Convert compiler warnings into errors (default: false).
- `tlJdkRelease` (setting): JVM target version for the compiled bytecode (default: Some(8)).

### sbt-typelevel-github
`TypelevelGitHubPlugin`: Populates boilerplate settings assuming you are using GitHub.

- `tlGitHubRepo` (setting): The name of this repository on GitHub.
- `tlGitHubDev(user, fullName)` (method): Helper to create a `Developer` entry from a GitHub username.

`TypelevelScalaJSGitHubPlugin`: Points your sourcemaps to GitHub permalinks. Only activated for Scala.js projects.

### sbt-typelevel-github-actions
`GitHubActionsPlugin`: Provides general functionality, giving builds the ability to introspect on their host workflow and whether or not they are running in GitHub Actions.

`GenerativePlugin`: Makes it easier to maintain GitHub Actions builds for sbt projects by generating ci.yml and clean.yml workflow definition files.

Both plugins are documented in [**sbt-typelevel-github-actions**](gha.md).

### sbt-typelevel-ci
`TypelevelCiPlugin`: Sets up GitHub actions to run tests and submit dependencies for vulnerability scanning. You can optionally enable checks for headers, formatting, scalafix, MiMa, and scaladoc.

- `tlCiHeaderCheck` (setting): Whether to do header check in CI (default: `false`).
- `tlCiScalafmtCheck` (setting): Whether to do scalafmt check in CI (default: `false`).
- `tlCiJavafmtCheck` (setting): Whether to do javafmt check in CI (default: `false`).
- `tlCiScalafixCheck` (setting): Whether to do scalafix check in CI (default: `false`).
- `tlCiMimaBinaryIssueCheck` (setting): Whether to do MiMa binary issues check in CI (default: `false`).
- `tlCiDocCheck` (setting): Whether to build API docs in CI (default: `false`).
- `tlCiDependencyGraphJob` (setting): Whether to add a job to submit dependencies to GH (default: `true`)
- `tlCiStewardValidateConfig` (setting): The location of the Scala Steward config to validate (default: `.scala-steward.conf`, if exists).
- `tlCrossRootProject` (method): helper to create a `root` project that can aggregate both `Project`s and `CrossProject`s. Automatically creates separate jobs in the CI matrix for each platform (JVM, JS, etc.).

### sbt-typelevel-sonatype-ci-release
`TypelevelSonatypeCiReleasePlugin`: Sets up GitHub actions to publish to Sonatype in CI.

- Requires the `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` secrets
- `tlCiReleaseTags` (setting): Controls whether or not v-prefixed tags should be released from CI (default `true`).
- `tlCiReleaseBranches` (setting): The branches in your repository to release from in CI on every push. Depending on your versioning scheme, they will be either snapshots or (hash) releases. Leave this empty if you only want CI releases for tags. (default: `[]`).
- `tlCiRelease` (command): Performs a `tlRelease` from the CI and reports to GH step summary.

### sbt-typelevel-ci-signing
`TypelevelCiSigningPlugin`: Sets up GitHub actions to sign your artifacts in CI.

- Requires `PGP_SECRET` secret, with your base64-encoded PGP key
- Optionally set the `PGP_PASSPHRASE` secret, but we do not recommend passphrase-protected keys for new projects. See discussion in [#9](https://github.com/typelevel/sbt-typelevel/discussions/9#discussioncomment-1251774).

### sbt-typelevel-ci-release
`TypelevelCiReleasePlugin`: The super-plugin that sets you up with versioning, mima, signing, and sonatype publishing, all in GitHub actions.
Using this plugin the following 2 settings have new default values:

- `tlCiMimaBinaryIssueCheck` (setting): Whether to do MiMa binary issues check in CI (default: `true`).
- `tlCiDocCheck` (setting): Whether to build API docs in CI (default: `true`).

### sbt-typelevel-scalafix
`TypelevelScalafixPlugin`

- `tlTypelevelScalafixVersion` (setting): The version of typelevel-scalafix to add to the scalafix dependency classpath.

### sbt-typelevel
`TypelevelPlugin`: The super-super-plugin intended for bootstrapping the typical Typelevel project. Sets up CI release including snapshots, scalac settings, headers, and formatting.

`TypelevelBspPlugin`: A plugin that controls for which cross-project platforms the `bspEnabled` setting should be set to `true`. By default it becomes enabled for `JVMPlatform` only.

- `tlBspCrossProjectPlatforms` (setting): A set of platforms for which BSP should be enabled (default: not initialized)

### sbt-typelevel-site
`TypelevelSitePlugin`: Sets up an [mdoc](https://scalameta.org/mdoc/)/[Laika](https://typelevel.org/Laika/)-generated website, automatically published to GitHub pages in CI.

- `tlSiteHelium` (setting): The Helium theme configuration and extensions.
- `tlSiteIsTypelevelProject` (setting): Indicates whether the generated site should be pre-populated with UI elements specific to Typelevel Organization or Affiliate projects (default: None).
- `tlSiteApiUrl` (setting): URL to the API docs (default: `None`).
- `tlSiteApiModule` (setting): The module that publishes API docs (default: `None`).
- `tlSiteApiPackage` (setting): The top-level package for your API docs (e.g. org.typlevel.sbt).
- `tlSiteKeepFiles` (setting): Whether to keep existing files when deploying site (default: `true`).
- `tlSiteJavaVersion` (setting): The Java version to use for the site job, must be >= 11 (default: first compatible choice from `githubWorkflowJavaVersions`, otherwise Temurin 11).
- `tlSiteGenerate` (setting): A sequence of workflow steps which generates the site (default: `[Sbt(List("tlSite"))]`).
- `tlSitePublish` (setting): A sequence of workflow steps which publishes the site (default: `peaceiris/actions-gh-pages`).
- `tlSitePublishBranch` (setting): The branch to publish the site from on every push. Set this to `None` if you only want to update the site on tag releases. (default: `main`)
- `tlSitePublishTags` (setting): Defines whether the site should be published on tag releases. Note on setting this to `true` requires the `tlSitePublishBranch` setting to be set to `None`. (default: `false`)
- `tlSite` (task): Generate the site (default: runs mdoc then laika).
- `tlSitePreview` (task): Start a live-reload preview server (combines mdoc --watch with laikaPreview).

### sbt-typelevel-mergify
`MergifyPlugin`: Sets up .mergify.yml file generation

- `mergifyPrRules` (setting): The mergify pull request rules.
- `mergifyStewardConfig` (setting): Config for the automerge rule for Scala Steward PRs, set to `None` to disable.
- `mergifyRequiredJobs` (setting): Ids for jobs that must succeed for merging (default: `[build]`).
- `mergifySuccessConditions` (setting): Success conditions for merging (default: auto-generated from `mergifyRequiredJobs` setting).
- `mergifyGenerate` (task): Generates (and overwrites if extant) a .mergify.yml according to configuration.
- `mergifyCheck` (task): Checks to see if the .mergify.yml files are equivalent to what would be generated and errors if otherwise.

## Dependency diagram

sbt-typelevel plugins are in red and the super-plugins are boxed.

<a href="plugins.svg"><img src="plugins.svg" style="width: 100%"/></a>
