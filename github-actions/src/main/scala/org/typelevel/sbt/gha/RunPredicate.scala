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

/**
 * A predicate on a workflow run, built from the ref and the event that started it. It compiles
 * to a GitHub Actions expression.
 */
sealed abstract class RunPredicate extends Product with Serializable {
  def &&(that: RunPredicate): RunPredicate = RunPredicate.And(this, that)
  def ||(that: RunPredicate): RunPredicate = RunPredicate.Or(this, that)
  def unary_! : RunPredicate = RunPredicate.Not(this)
}

object RunPredicate {
  case object Always extends RunPredicate
  case object Never extends RunPredicate

  /**
   * Holds when `github.ref` matches the predicate.
   */
  final case class Ref(predicate: RefPredicate) extends RunPredicate

  object Ref {

    /**
     * Holds when `github.ref` matches any of the predicates.
     */
    def anyOf(predicates: Seq[RefPredicate]): RunPredicate =
      predicates.map(Ref(_)).reduceLeftOption[RunPredicate](_ || _).getOrElse(Never)
  }

  /**
   * Holds when `github.event_name` is the given event, such as `pull_request`.
   */
  final case class Event(name: String) extends RunPredicate

  /**
   * A GitHub Actions expression, written without the surrounding delimiters.
   */
  final case class Custom(expression: String) extends RunPredicate

  final case class And(left: RunPredicate, right: RunPredicate) extends RunPredicate
  final case class Or(left: RunPredicate, right: RunPredicate) extends RunPredicate
  final case class Not(predicate: RunPredicate) extends RunPredicate
}
