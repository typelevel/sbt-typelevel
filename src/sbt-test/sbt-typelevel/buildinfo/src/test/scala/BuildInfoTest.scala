package binfo

import org.scalacheck._
import org.scalacheck.Prop._
import org.scalacheck.util.Pretty

object BuildInfoTest extends Properties("buildinfo") {
  def field[A <% Pretty](str: String, ref: A)(accessor: BuildInfo.type => A) =
    property(str) = { accessor(BuildInfo) ?= ref }

  field("name", "binfo") { _.name }
  field("version", "0.1-SNAPSHOT") { _.version }
  field("scalaVersion", "2.10.3") { _.scalaVersion }
  field("sbtVersion", "0.13.5") { _.sbtVersion }
  field("scalaBinaryVersion", "2.10") { _.scalaBinaryVersion }
  field("vcsHash", vcsHash) { _.vcsHash }
}
