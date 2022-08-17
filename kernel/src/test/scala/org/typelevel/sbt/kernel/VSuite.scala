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

package org.typelevel.sbt.kernel

import munit.FunSuite

class VSuite extends FunSuite {

  test("1.1 needs bincompat with 1.0") {
    val currentV = V(1, 1, None, None)
    val prevV = V(1, 0, None, None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), true)
  }

  test("1.1 does not need bincompat with 1.2") {
    val currentV = V(1, 1, None, None)
    val nextV = V(1, 2, None, None)
    assertEquals(currentV.mustBeBinCompatWith(nextV), false)
  }

  test("2.0 does not need bincompat with 1.9") {
    val currentV = V(2, 0, None, None)
    val prevV = V(1, 9, None, None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), false)
  }

  test("1.1.1 needs bincompat with 1.1.0") {
    val currentV = V(1, 1, Some(1), None)
    val prevV = V(1, 1, Some(0), None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), true)
  }

  test("1.1 does not need bincompat with 1.0-M5") {
    val currentV = V(1, 1, None, None)
    val prevV = V(1, 0, None, Some("M5"))
    assertEquals(currentV.mustBeBinCompatWith(prevV), false)
  }

  test("0.5 does not need bincompat with 0.4") {
    val currentV = V(0, 5, None, None)
    val prevV = V(0, 4, None, None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), false)
  }

  test("0.5.1 needs bincompat with 0.5.0") {
    val currentV = V(0, 5, Some(1), None)
    val prevV = V(0, 5, Some(0), None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), true)
  }

  test("0.5.1 needs bincompat with 0.5.0") {
    val currentV = V(0, 5, Some(1), None)
    val prevV = V(0, 5, Some(0), None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), true)
  }

  test("0.0.2 needs bincompat with 0.0.1") {
    val currentV = V(0, 0, Some(1), None)
    val prevV = V(0, 0, Some(0), None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), true)
  }

  test("x.y needs bincompat with self") {
    val v0 = V(0, 5, None, None)
    assertEquals(v0.mustBeBinCompatWith(v0), true)
    val v1 = V(1, 5, None, None)
    assertEquals(v1.mustBeBinCompatWith(v1), true)
  }

  test("x.y.1 does not need bincompat with x.y") {
    val currentV = V(1, 5, Some(1), None)
    val prevV = V(1, 5, None, None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), false)
    val currentV0 = V(0, 5, Some(1), None)
    val prevV0 = V(0, 5, None, None)
    assertEquals(currentV0.mustBeBinCompatWith(prevV0), false)
  }

  test("x.y.1 < x.y") {
    val patch0 = V(0, 5, Some(1), None)
    val nopatch0 = V(0, 5, None, None)
    assertEquals(patch0 < nopatch0, true)
    val patch1 = V(1, 5, Some(1), None)
    val nopatch1 = V(1, 5, None, None)
    assertEquals(patch1 < nopatch1, true)
  }

}
