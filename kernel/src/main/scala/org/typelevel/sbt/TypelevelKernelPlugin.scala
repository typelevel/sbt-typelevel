package org.typelevel.sbt

import sbt._, Keys._

object TypelevelKernelPlugin extends AutoPlugin {

  override def requires = sbt.plugins.CorePlugin
  override def trigger = allRequirements

  object autoImport {
    def replaceCommandAlias(name: String, contents: String): Seq[Setting[State => State]] =
      Seq(GlobalScope / onLoad ~= { (f: State => State) =>
        f andThen { s: State =>
          BasicCommands.addAlias(BasicCommands.removeAlias(s, name), name, contents)
        }
      })
  }

}
