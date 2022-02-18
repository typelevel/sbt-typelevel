/*
 * Copyright 2022 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.sbt.gha

final case class JavaSpec(dist: JavaSpec.Distribution, version: String) {
  def render: String = dist match {
    case JavaSpec.Distribution.GraalVM(gversion) => s"graal_$gversion@$version"
    case dist => s"${dist.rendering}@$version"
  }
}

object JavaSpec {

  def temurin(version: String): JavaSpec = JavaSpec(Distribution.Temurin, version)
  def graalvm(version: String): JavaSpec = JavaSpec(Distribution.GraalVM, version)

  @deprecated(
    "Use single-arg overload to get the latest GraalVM for a Java version. If you need a specific GraalVM then use JavaSpec(Distribution.GraalVM(graal), version)",
    "0.4.6")
  def graalvm(graal: String, version: String): JavaSpec =
    JavaSpec(Distribution.GraalVM(graal), version)

  sealed abstract class Distribution(val rendering: String) extends Product with Serializable {
    private[gha] def isTlIndexed: Boolean = false
  }

  // marker for distributions in the typelevel jdk index
  private[gha] sealed abstract class TlDistribution(rendering: String)
      extends Distribution(rendering) {
    override def isTlIndexed = true
  }

  object Distribution {
    case object Temurin extends TlDistribution("temurin")
    case object Corretto extends TlDistribution("corretto")
    case object GraalVM extends TlDistribution("graalvm")
    case object OpenJ9 extends TlDistribution("openj9")
    case object Oracle extends TlDistribution("oracle")

    case object Zulu extends Distribution("zulu")
    @deprecated("AdoptOpenJDK been transitioned to Adoptium Temurin", "0.4.6")
    case object Adopt extends Distribution("adopt-hotspot")
    case object Liberica extends Distribution("liberica")
    final case class GraalVM(version: String) extends Distribution(version)
  }
}
