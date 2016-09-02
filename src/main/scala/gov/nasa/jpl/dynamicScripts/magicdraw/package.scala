/*
 * Copyright 2014 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
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
 * License Terms
 */

package gov.nasa.jpl.dynamicScripts

import java.util.concurrent.TimeUnit

import scala.collection.immutable._
import scala.concurrent.duration.FiniteDuration
import scala.{Boolean, Long, StringContext}
import scala.Predef.{refArrayOps, String}

package object magicdraw {

  /**
    * Adapted from a Java algorithm
    *
    * @see http://www.adarshr.com/simple-implementation-of-wildcard-text-matching-using-java
    */
  def wildCardMatch( text: String, pattern: String ): Boolean = {

    def wildCardMatch( matchHead: Boolean, anyTail: Boolean, cards: List[String], text: String ): Boolean =
      cards match {
        case Nil =>
          anyTail || text.isEmpty
        case c :: cs =>
          text.indexOf( c ) match {
            case -1 =>
              false
            case idx =>
              if ( matchHead && ( idx > 1 ) )
                false
              else
                wildCardMatch( false, anyTail, cs, text.substring( idx + c.length ) )
          }
      }

    wildCardMatch( true, pattern.endsWith( "*" ), pattern.split( "\\*" ).toList, text )
  }

  def prettyDurationFromTo(from: Long, to: Long): String = {

    val d = FiniteDuration(to - from, TimeUnit.MILLISECONDS)

    val (hours, minutes, seconds, millis) =
      (d.toHours, d.toMinutes, d.toSeconds, d.toMillis)

    val adjMinutes = minutes - hours * 60
    val adjSeconds = seconds - minutes * 60
    val adjMillis = millis - seconds * 1000

    val r1 = if (hours > 0) s"$hours h" else ""
    val r2 = if (adjMinutes > 0) (if (!r1.isEmpty) r1+", " else "") + s"$adjMinutes m" else r1
    val r3 = if (adjSeconds > 0) (if (!r2.isEmpty) r2+", " else "") + s"$adjSeconds s" else r2
    val r4 = if (adjMillis > 0) (if (!r3.isEmpty) r3+", " else "") + s"$adjMillis ms" else r3
    val r5 = if (r4.isEmpty) "<1 ms" else r4
    r5
  }

}