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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes

import java.awt.event.ActionEvent
import java.lang.Object
import javax.swing.Icon

import com.nomagic.magicdraw.ui.actions.DiagramContextToolbarAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

import scala.Unit
import scala.Predef.{String}
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicShapeDiagramContextToolbarAction
( diagram: DiagramPresentationElement,
  finalizationAction: internal.DrawShapeDiagramActionHelper.DynamicShapeFinalizationAction)
  extends DiagramContextToolbarAction(
    finalizationAction.action.name.hname,
    finalizationAction.action.name.hname,
    DynamicScriptsPlugin.getInstance().getDynamicScriptIcon(finalizationAction.action)) {
  
  override def updateState(): Unit = {
    super.updateState()
    val isEnabled =
      finalizationAction.isEnabled &&
      MDUML.isAccessCompatibleWithElements( finalizationAction.action.access, diagram )
    setEnabled(isEnabled)
  }
  
  override def getDescription: String =
    finalizationAction.action.prettyPrint("  ")
    
  override def actionPerformed(ev: ActionEvent): Unit = {
    val drawRelatinshipPathAction =
      internal.DrawShapeDiagramActionHelper.DrawDynamicShapeAction(
        finalizationAction,
        getDiagram(),
        getID + ".DrawShapeAction",
        getName,
        null,
        getLargeIcon)
    drawRelatinshipPathAction.actionPerformed(ev)
  }
  
  override def getSmallIcon: Icon =
    getLargeIcon
  
  override def getDiagram: DiagramPresentationElement =
    diagram
  
  override def clone(): Object =
    copy()
  
}