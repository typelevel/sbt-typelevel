package org.typelevel.sbt

import sbt._

object TypelevelProjects {
  val Cats = tl("cats")
  val CatsEffect = tl("cats-effect")
  val Discipline = gh("discipline")
  val Fs2 = "fs2" -> url("https://fs2.io/")
  val Http4s = "http4s" -> url("https://http4s.org/")
  val Scalacheck = "scalacheck" -> url("https://scalacheck.org/")
  val Shapeless = gh("shapeless", "milessabin")
  val Shapeless3 = gh("shapeless-3")
  
  private def tl(repo: String) = repo -> url(s"https://typelevel.org/$repo/")
  private def gh(repo: String, user: String = "typelevel") = repo -> url(s"https://github.com/$user/$repo/")
}
