This branch has a working version of SBT typelevel on SBT 2.

It incorporates changes from the following PRs.
 - [Replace sbt-gpg with sbt-pgp](https://github.com/typelevel/sbt-typelevel/pull/907). 
 - [Cross build modules for SBT 2](https://github.com/typelevel/sbt-typelevel/pull/912), which cross builds non-site modules. 

It uses a local snapshot of Laika on SBT 2 to build site modules.

# How to upgrade projects

Follow the steps under [docs/sbt-2-migration.md](docs/sbt-2-migration.md).

# Status

The following projects have been migrated locally:

 - [scalacheck](https://github.com/typelevel/scalacheck/compare/main...zainab-ali:sbt-2)
 - [discipline](https://github.com/typelevel/discipline/compare/main...zainab-ali:sbt-2)
 - [cats](https://github.com/typelevel/cats/compare/main...zainab-ali:sbt-2)
 - [cats-effect](https://github.com/typelevel/cats-effect/compare/series/3.x...zainab-ali:sbt-2)
 - [weaver](https://github.com/typelevel/weaver-test/compare/main...zainab-ali:sbt-2)

Note that while there are no issues with `sbt-typelevel`, there may be issues with the custom build logic in any of these projects.
