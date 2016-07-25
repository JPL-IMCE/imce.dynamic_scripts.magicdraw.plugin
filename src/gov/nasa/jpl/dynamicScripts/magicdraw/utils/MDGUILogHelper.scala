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
package gov.nasa.jpl.dynamicScripts.magicdraw.utils


import com.nomagic.magicdraw.core.{Application, GUILog}
import com.nomagic.magicdraw.uml.actions.SelectInContainmentTreeRunnable
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import org.apache.log4j.Logger

import scala.collection.JavaConversions.mapAsJavaMap
import scala.{AnyVal, Unit}
import scala.Predef.{ArrowAssoc, String}

@scala.deprecated("", "")
class MDGUILogHelper(val guiLog: GUILog) extends AnyVal {

  def getMDPluginsLog: Logger = MDLog.getPluginsLog

  /**
    * The format string is expected to have one or more '<A>%s</A>' hyperlink format specifiers
    * for a corresponding String component in the sequence of tuple links.
    * There should be as many hyperlink format specifiers as tuples in the links sequence.
    * The MD GUI Log will show the formatted, hyperlinked string obtained by replacing
    * each Java format specifier with the corresponding String component of each tuple in the sequence of links.
    */
  def addGUILogHyperlink( format: String, links: (String, Element)*): Unit = {
    val linkedFormat = java.lang.String.format(
      format,
      links.map { case ( (label:String, element: Element) ) => label } : _*)
    val linkMap =
      links
        .map { case ( (label:String, element: Element) ) => label -> new SelectInContainmentTreeRunnable( element ) }
        .toMap
    guiLog.addHyperlinkedText( linkedFormat, linkMap )
  }

}

object MDGUILogHelper {

  def getGUILog: GUILog = Application.getInstance.getGUILog

  implicit def toMDGUILogHelper(guiLog: GUILog): MDGUILogHelper =
  new MDGUILogHelper(guiLog)

}