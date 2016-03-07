/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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