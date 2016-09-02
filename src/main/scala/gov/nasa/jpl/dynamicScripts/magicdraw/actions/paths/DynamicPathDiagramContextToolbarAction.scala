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

package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths

import java.lang.Object
import java.awt.event.ActionEvent

import javax.swing.Icon

import com.nomagic.magicdraw.ui.actions.DiagramContextToolbarAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

import scala.{Unit}
import scala.Predef.String
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicPathDiagramContextToolbarAction
( diagram: DiagramPresentationElement,
  firstSelected: PresentationElement,
  finalizationAction: internal.DrawPathDiagramActionHelper.DynamicPathFinalizationAction)
  extends DiagramContextToolbarAction(
    finalizationAction.action.name.hname,
    finalizationAction.action.name.hname,
    DynamicScriptsPlugin.getInstance().getDynamicScriptIcon(finalizationAction.action)) {
  
  setFakeMouseEvents(true)
  
  override def updateState(): Unit = {
    super.updateState()
    setEnabled(
      finalizationAction.isEnabled &&
      MDUML.isAccessCompatibleWithElements( finalizationAction.action.access, diagram, firstSelected ))
  }
  
  override def getDescription: String =
    finalizationAction.action.prettyPrint("  ")
    
  override def actionPerformed
  (ev: ActionEvent)
  : Unit = {
    val drawRelatinshipPathAction =
      internal.DrawPathDiagramActionHelper.DrawDynamicPathAction(
        finalizationAction,
        getDiagram,
        getID + ".DrawPathAction",
        getName,
        null,
        getLargeIcon)
    drawRelatinshipPathAction.actionPerformed(ev)
    super.actionPerformed(ev)
  }
  
  override def getSmallIcon: Icon =
    getLargeIcon
  
  override def getFirstSelected: PresentationElement =
    firstSelected
  
  override def clone(): Object =
    copy()
  
}