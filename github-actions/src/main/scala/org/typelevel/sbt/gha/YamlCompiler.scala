package org.typelevel.sbt.gha

private[sbt] object YamlCompiler {

  def indent(output: String, level: Int): String = {
    val space = (0 until level * 2).map(_ => ' ').mkString
    (space + output.replace("\n", s"\n$space")).replaceAll("""\n[ ]+\n""", "\n\n")
  }

  def isSafeString(str: String): Boolean =
    !(str.indexOf(':') >= 0 || // pretend colon is illegal everywhere for simplicity
      str.indexOf('#') >= 0 || // same for comment
      str.indexOf('!') == 0 ||
      str.indexOf('*') == 0 ||
      str.indexOf('-') == 0 ||
      str.indexOf('?') == 0 ||
      str.indexOf('{') == 0 ||
      str.indexOf('}') == 0 ||
      str.indexOf('[') == 0 ||
      str.indexOf(']') == 0 ||
      str.indexOf(',') == 0 ||
      str.indexOf('|') == 0 ||
      str.indexOf('>') == 0 ||
      str.indexOf('@') == 0 ||
      str.indexOf('`') == 0 ||
      str.indexOf('"') == 0 ||
      str.indexOf('\'') == 0 ||
      str.indexOf('&') == 0)

  def wrap(str: String): String =
    if (str.indexOf('\n') >= 0)
      "|\n" + indent(str, 1)
    else if (isSafeString(str))
      str
    else
      s"'${str.replace("'", "''")}'"

  def compileList(items: List[String], level: Int, maxLength: Int = 40): String = {
    val rendered = items.map(wrap)
    if (rendered.map(_.length).sum < maxLength) // just arbitrarily...
      rendered.mkString(" [", ", ", "]")
    else
      "\n" + indent(rendered.map("- " + _).mkString("\n"), level)
  }

  def compileListOfSimpleDicts(items: List[Map[String, String]]): String =
    items map { dict =>
      val rendered = dict map { case (key, value) => s"$key: $value" } mkString "\n"

      "-" + indent(rendered, 1).substring(1)
    } mkString "\n"

}
