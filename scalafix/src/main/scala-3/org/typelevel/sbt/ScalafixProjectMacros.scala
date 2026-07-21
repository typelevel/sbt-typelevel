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
import scala.quoted.*
import scala.annotation.tailrec


private[sbt] trait ScalafixProjectMacros {
  // Adapted from sbt.std.KeyMacro.projectImpl
  inline def scalafixProject: ScalafixProject = ${ ScalafixProjectMacros.scalafixProjectImpl }
}

private[sbt] object ScalafixProjectMacros {
  def scalafixProjectImpl(using Quotes): Expr[ScalafixProject] =
    val name = definingValName
    '{ ScalafixProject($name) }

  private def definingValName(using q: Quotes): Expr[String] =
    val term = enclosingTerm
    if term.isValDef then Expr(term.name)
    else q.reflect.report.errorAndAbort(errorMsg)

  private def enclosingTerm(using qctx: Quotes) =
    import qctx.reflect.*
    @tailrec
    def enclosingTerm0(sym: Symbol): Symbol =
      sym match
        case sym if sym.flags.is(Flags.Macro)     => enclosingTerm0(sym.owner)
        case sym if sym.flags.is(Flags.Synthetic) => enclosingTerm0(sym.owner)
        case sym if !sym.isTerm                   => enclosingTerm0(sym.owner)
        case _                                    => sym
    enclosingTerm0(Symbol.spliceOwner)

  private def errorMsg: String = "tlScalafixProject must be directly assigned to a val, such as `val x = tlScalafixProject`. Alternatively, you can use `org.typelevel.sbt.ScalafixProject.apply`"
}
