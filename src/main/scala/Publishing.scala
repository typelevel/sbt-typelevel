package org.typelevel.sbt

import scala.sys.props

import sbt._

object Publishing {

  def fromFile: Option[Credentials] =
    props.get("build.publish.credentials").map(file => Credentials(new File(file)))

  def fromUserPass: Option[Credentials] =
    (props.get("build.publish.user"), props.get("build.publish.password")) match {
      case (Some(user), Some(pass)) =>
        val realm = props.get("build.publish.realm").getOrElse("Sonatype Nexus Repository Manager")
        val host = props.get("build.publish.host").getOrElse("oss.sonatype.org")
        Some(Credentials(realm, host, user, pass))
      case _ =>
        None
    }

  def fromFallbackFile: Option[Credentials] = {
    val file = Path.userHome / ".ivy2" / ".credentials"
    if (file.exists())
      Some(Credentials(file))
    else
      None
  }

}
