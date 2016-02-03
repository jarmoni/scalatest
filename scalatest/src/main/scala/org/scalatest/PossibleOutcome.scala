/*
 * Copyright 2001-2016 Artima, Inc.
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
package org.scalatest

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

class PossibleOutcome(underlying: Future[Outcome]) {
  // TODO: add tests for pretty toString

  def onCompletedThen(f: Try[Outcome] => Unit)(implicit executionContext: ExecutionContext): PossibleOutcome = {
    PossibleOutcome {
      underlying map { outcome =>
        f(Success(outcome))
        outcome
      }
    }
  }

  def toFuture: Future[Outcome] = underlying
}

object PossibleOutcome {
  def apply(underlying: Future[Outcome]): PossibleOutcome = new PossibleOutcome(underlying)
}


