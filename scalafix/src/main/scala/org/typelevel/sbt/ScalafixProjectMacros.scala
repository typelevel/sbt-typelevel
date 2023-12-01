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

package org.typelevel.sbt

import scala.annotation.tailrec
import scala.reflect.macros.blackbox

private[sbt] object ScalafixProjectMacros {

  // Copied from sbt.std.KeyMacro
  def definingValName(c: blackbox.Context, invalidEnclosingTree: String => String): String = {
    import c.universe.{Apply => ApplyTree, _}
    val methodName = c.macroApplication.symbol.name
    def processName(n: Name): String =
      n.decodedName
        .toString
        .trim // trim is not strictly correct, but macros don't expose the API necessary
    @tailrec def enclosingVal(trees: List[c.Tree]): String = {
      trees match {
        case ValDef(_, name, _, _) :: _ => processName(name)
        case (_: ApplyTree | _: Select | _: TypeApply) :: xs => enclosingVal(xs)
        // lazy val x: X = <methodName> has this form for some reason (only when the explicit type is present, though)
        case Block(_, _) :: DefDef(mods, name, _, _, _, _) :: _ if mods.hasFlag(Flag.LAZY) =>
          processName(name)
        case _ =>
          c.error(c.enclosingPosition, invalidEnclosingTree(methodName.decodedName.toString))
          "<error>"
      }
    }
    enclosingVal(enclosingTrees(c).toList)
  }

  // Copied from sbt.std.KeyMacro
  def enclosingTrees(c: blackbox.Context): Seq[c.Tree] =
    c.asInstanceOf[reflect.macros.runtime.Context]
      .callsiteTyper
      .context
      .enclosingContextChain
      .map(_.tree.asInstanceOf[c.Tree])

  def scalafixProjectImpl(c: blackbox.Context): c.Expr[ScalafixProject] = {
    import c.universe._

    val enclosingValName = definingValName(
      c,
      methodName =>
        s"""$methodName must be directly assigned to a val, such as `val x = $methodName`. Alternatively, you can use `org.typelevel.sbt.ScalafixProject.apply`"""
    )

    val name = c.Expr[String](Literal(Constant(enclosingValName)))

    reify { ScalafixProject(name.splice) }
  }
}
