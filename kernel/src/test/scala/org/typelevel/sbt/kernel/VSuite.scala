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
import scala.util.Random

class VSuite extends FunSuite {

  test("V.apply constructs V") {
    assertEquals(V("0.0"), Some(V(0, 0, None, None)))
    assertEquals(V("0.0-M1"), Some(V(0, 0, None, Some("M1"))))
    assertEquals(V("0.0.0-M1"), Some(V(0, 0, Some(0), Some("M1"))))
    assertEquals(V("10.0"), Some(V(10, 0, None, None)))
    assertEquals(V("10.100"), Some(V(10, 100, None, None)))
    assertEquals(V("10.100.1000"), Some(V(10, 100, Some(1000), None)))
  }

  test("x.y.z-M1 is a prerelease") {
    assert(V(0, 0, None, Some("M1")).isPrerelease)
    assert(V(0, 0, Some(1), Some("M1")).isPrerelease)
    assert(V(0, 1, None, Some("M1")).isPrerelease)
    assert(V(0, 1, Some(1), Some("M1")).isPrerelease)
    assert(V(1, 0, None, Some("M1")).isPrerelease)
    assert(V(1, 0, Some(1), Some("M1")).isPrerelease)
    assert(V(1, 1, None, Some("M1")).isPrerelease)
    assert(V(1, 1, Some(1), Some("M1")).isPrerelease)
  }

  test("x.y.2 is the same series as x.y.1") {
    assert(V(0, 0, Some(1), None).isSameSeries(V(0, 0, Some(2), None)))
    assert(V(0, 1, Some(1), None).isSameSeries(V(0, 1, Some(2), None)))
    assert(V(1, 1, Some(1), None).isSameSeries(V(1, 1, Some(2), None)))
  }

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

  test("0.0.2 does not need bincompat with 0.0.1") {
    val currentV = V(0, 0, Some(1), None)
    val prevV = V(0, 0, Some(0), None)
    assertEquals(currentV.mustBeBinCompatWith(prevV), false)
  }

  test("all versions > 0.0 that are not prereleases need bincompat with self") {
    val vs = List(
      V(0, 5, None, None),
      V(0, 5, Some(1), None),
      V(1, 0, None, None),
      V(1, 0, Some(1), None),
      V(1, 5, None, None),
      V(1, 5, Some(1), None)
    )
    vs.foreach(v => assert(v.mustBeBinCompatWith(v), s"$v did not need bincompat with itself"))
  }

  test("all versions < 0.0 need bincompat with self".fail) {
    val vs = List(
      V(0, 0, None, None),
      V(0, 0, Some(1), None)
    )
    vs.foreach(v => assert(v.mustBeBinCompatWith(v), s"$v did not need bincompat with itself"))
  }

  // We current don't compare prereleases correctly
  test("all versions that are prerelease need bincompat with self".fail) {
    val vs = List(
      V(0, 0, None, Some("M1")),
      V(0, 0, Some(1), Some("M1")),
      V(0, 5, None, Some("M1")),
      V(0, 5, Some(1), Some("M1")),
      V(1, 0, None, Some("M1")),
      V(1, 0, Some(1), Some("M1")),
      V(1, 5, None, Some("M1")),
      V(1, 5, Some(1), Some("M1"))
    )
    vs.foreach(v => assert(v.mustBeBinCompatWith(v), s"$v did not need bincompat with itself"))
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

  test("x.y.1 < x.y.2") {
    val patch0 = V(0, 5, Some(1), None)
    val p1patch0 = V(0, 5, Some(2), None)
    assertEquals(patch0 < p1patch0, true)
    val patch1 = V(1, 5, Some(1), None)
    val p1patch1 = V(1, 5, Some(2), None)
    assertEquals(patch1 < p1patch1, true)
  }

  test("x.y.1-M1 < x.y.1-RC1 < x.y.1") {
    val pre0 = V(0, 5, Some(1), Some("M1"))
    val nopre0 = V(0, 5, Some(1), None)
    assertEquals(pre0 < nopre0, true)
    val pre1 = V(1, 5, Some(1), Some("M1"))
    val nopre1 = V(1, 5, Some(1), None)
    assertEquals(pre1 < nopre1, true)

    val release = V(0, 1, Some(1), None)
    val rc1 = V(0, 1, Some(1), Some("RC1"))
    val rc2 = V(0, 1, Some(1), Some("RC2"))
    val m1 = V(0, 1, Some(1), Some("M1"))
    val m2 = V(0, 1, Some(1), Some("M2"))
    val expected = List(m1, m2, rc1, rc2, release)
    assertEquals(List(rc2, m1, release, m2, rc1).sorted, expected)
    assertEquals(List(release, rc1, rc2, m2, m1).sorted, expected)

    val random = Random.shuffle(List(release, rc1, rc2, m2, m1))
    assertEquals(clue(random).sorted, expected)
  }
}
