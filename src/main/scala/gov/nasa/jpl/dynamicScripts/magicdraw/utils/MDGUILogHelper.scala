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
class MDGUILogHelper(@scala.transient val guiLog: GUILog) extends AnyVal {

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