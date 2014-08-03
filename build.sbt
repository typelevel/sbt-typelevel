sbtPlugin := true

name := "sbt-typelevel"

organization := "org.typelevel"

version := "0.4-SNAPSHOT"

licenses := Seq("Apache 2" → url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("http://typelevel.org/"))

resolvers += Resolver.sonatypeRepo("releases")

// This is both a plugin and a meta-plugin

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.3")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")

// Publishing

publishTo <<= (version).apply { v =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("Snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("Releases" at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(
  Option(System.getProperty("build.publish.credentials")) map (new File(_)) getOrElse (Path.userHome / ".ivy2" / ".credentials")
)

pomIncludeRepository := Function.const(false)

pomExtra := (
  <scm>
    <url>https://github.com/typelevel/sbt-typelevel</url>
    <connection>scm:git:git://github.com/typelevel/sbt-typelevel.git</connection>
  </scm>
  <developers>
    <developer>
      <id>larsrh</id>
      <name>Lars Hupel</name>
      <url>https://github.com/larsrh</url>
    </developer>
  </developers>
)
