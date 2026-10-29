This branch has a working version of SBT typelevel on SBT 2.

It incorporates changes from the following PRs.
 - [Replace sbt-gpg with sbt-pgp](https://github.com/typelevel/sbt-typelevel/pull/907). 
 - [Cross build modules for SBT 2](https://github.com/typelevel/sbt-typelevel/pull/912), which cross builds non-site modules. 

It uses a local snapshot of Laika on SBT 2 to build site modules.

# How to upgrade projects

Follow the steps under [docs/sbt-2-migration.md](docs/sbt-2-migration.md).
