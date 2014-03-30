package org.typelevel.sbt

case class Developer(name: String, id: String) {

  def pomExtra: xml.NodeSeq =
    <developer>
      <id>{ id }</id>
      <name>{ name }</name>
      <url>http://github.com/{ id }</url>
    </developer>

}
