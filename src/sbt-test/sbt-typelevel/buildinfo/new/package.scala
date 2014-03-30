import scala.sys.process._

package object binfo {
  def vcsHash: Option[String] = Some("git rev-parse HEAD".!!.trim)
}
