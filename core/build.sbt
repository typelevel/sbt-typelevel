addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
addSbtPlugin("com.github.sbt" % "sbt-header" % "5.11.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.4.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.4.0")

// TODO: Zainab - These exclusion rules are only for the sbt-typelevel build. Why are they necessary?
excludeDependencies ++= {
  Seq(
    ExclusionRule(
      "org.scala-lang.modules",
      "scala-collection-compat_2.13"
    ),
    ExclusionRule("org.scala-lang.modules", "scala-xml_2.13")
  )
}
