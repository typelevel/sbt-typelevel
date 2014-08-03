sbt-typelevel
=============

SBT plugin which understands binary compatibility

[![Build Status](https://secure.travis-ci.org/typelevel/sbt-typelevel.png?branch=master)](http://travis-ci.org/typelevel/sbt-typelevel)


Purpose
-------

`sbt-typelevel` tries to provide a set of good standards for some widely-used SBT plugins, in order to enforce policies with respect to:
* versioning
* binary compatibility
* releasing
* version control system integration

Because of that, `sbt-typelevel` is a meta-plugin and pulls in the following plugins into your build:
* [`sbt-pgp`](https://github.com/sbt/sbt-pgp)
  Used to sign artifacts when uploading to Sonatype.
* [`sbt-release`](https://github.com/sbt/sbt-release/)
  Provides a workflow for cutting releases, including bumping of version numbers, and tagging in the repository.
* [`sbt-mima-plugin`](https://github.com/typesafehub/migration-manager)
  In a stable branch of a library, ensure that newer versions are backward binary compatible to older versions.
* [`sbt-dependency-graph`](https://github.com/jrudolph/sbt-dependency-graph)
  Prints a graph of all transitive dependencies. Here, just used to extract all transitive dependencies from the Ivy resolution reports.
* [`sbt-buildinfo`](https://github.com/sbt/sbt-buildinfo)
  Generates a source file containing information about the build, such as the Scala version.
* [`sbt-sonatype`](https://github.com/xerial/sbt-sonatype)
  Publishing defaults for Sonatype.


Usage
-----

`sbt-typelevel` is available for SBT 0.13.x. To use it, add it to your `project/plugins.sbt`:

```scala
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.2.1")
```


The plugin provides three settings, which you can add to your `build.sbt`. *Read this carefully in order to not bloat your build or produce conflicts with other plugins.*

```scala
// useful if you're just consuming libraries, but don't publish yourself,
// don't care about binary compatibility etc.
typelevelConsumerSettings

// configures all the plugins mentioned above
// contains `typelevelConsumerSettings`
typelevelDefaultSettings

// should only be used for one submodule in the build
// configures `sbt-buildinfo`
typelevelBuildInfoSettings
```

*If in doubt, only use `typelevelConsumerSettings`.*

Please read on for further instructions, since some of these settings require additional settings to work properly.

### Versioning & Releasing

If you choose to use `typelevelDefaultSettings`, the version number of your project will be computed from a release series and a patch number. It is expected that these settings live in a file `version.sbt` in the root of your project:

```scala
import org.typelevel.sbt.ReleaseSeries
import org.typelevel.sbt.Version._

TypelevelKeys.series in ThisBuild := ReleaseSeries(2,3)

TypelevelKeys.relativeVersion in ThisBuild := Relative(0,Snapshot)
```

By default, the `release` command will perform the following steps:

1. Check if all dependencies are non-`SNAPSHOT`.
2. Run tests.
3. Ask for the patch number of the version which is to be released, and the following patch number.
4. Update the `version.sbt` file.
5. Commit changes, if working directory is not dirty with other changes.
6. Tag the current commit.
7. Publish (signed) artifacts.
8. Update the `version.sbt` file.
9. Commit changes.

Note that this will not automatically push the two commits and the tag to the remote repository; you have to do that yourself.

If you don't want tests to run, pass the `skip-tests` option to `release`. If you're publishing a cross-Scala-versioned project, pass the `cross` option to `release`. Both options can be combined:

```
sbt> release skip-tests cross
```

Typically, after a release, the `version.sbt` file will look like this:

```scala
import org.typelevel.sbt.ReleaseSeries
import org.typelevel.sbt.Version._

TypelevelKeys.series in ThisBuild := ReleaseSeries(2,3)

TypelevelKeys.relativeVersion in ThisBuild := Relative(1,Snapshot)

TypelevelKeys.lastRelease in ThisBuild := Relative(0,Final)
```

### Scaladoc

If you're hosting your project on GitHub, this plugin can automatically instruct Scaladoc to link to the sources.

```scala
// (user name, repository name)
TypelevelKeys.githubProject := ("example", "example-docs")
```

A released version (i.e. not a snapshot version) will link to the appropriate tag (which is assumed to be of the form `v0.1.2`).
Snapshot versions will be linked using their commit hash.

### Binary compatibility

To check if there are any problems with binary compatibility, run the command `mimaReportBinaryIssues`. This will work out of the box, no configuration needed.

### Dependency checks

The `typelevelConsumerSettings` provide the `checkDependencies` task. For example, if your library dependencies are this:

```scala
libraryDependencies += "org.scalaz" %% "scalaz-scalacheck-binding" % "7.0.5"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0"
```

Then, the plugin will tell you:

```
sbt> checkDependencies
[info] Compatible versions in org.scalacheck#scalacheck_2.10: 1.10.0 and 1.10.1
[info] Compatible versions in org.scala-lang#scala-library: 2.10.1 and 2.10.3
```

It can do that because it knows that both the Scala library and Scalacheck adhere to certain binary compatibility standards.

However, changing the Scalacheck version to `1.11.0` will print:

```
sbt> checkDependencies
[error] Version mismatch in org.scalacheck#scalacheck_2.10: 1.10.1 and 1.11.0 are different
[info] Compatible versions in org.scala-lang#scala-library: 2.10.1 and 2.10.3
[error] (*:checkDependencies) Dependency check failed, found 1 version mismatch(es)
```

It is important that you do not overwrite the `conflictManager` key in SBT. It will only give reliable results with the `latest` strategy, which is enabled by default.

Make sure to run `checkDependencies` in all relevant configurations, e.g. for your test dependencies:

```
sbt> test:checkDependencies
```

By default, it will only check the `compile` configuration.


Limitations
-----------

* The `typelevelConsumerSettings` pull the `release` command into your build, but it is not configured properly. You don't have to use that command.
* Both the build info and the releasing will only work if you're using either Git or Mercurial.
