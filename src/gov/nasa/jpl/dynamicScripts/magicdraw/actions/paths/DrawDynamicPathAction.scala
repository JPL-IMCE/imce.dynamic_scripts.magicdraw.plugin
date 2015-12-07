/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2015, California Institute of Technology ("Caltech").
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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths

import javax.swing.Icon
import javax.swing.KeyStroke
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.magicdraw.uml.symbols.paths.PathElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction
import scala.util.Success
import scala.util.Failure

/**
 * Normally, this class would extend: com.nomagic.magicdraw.ui.actions.DrawPathDiagramAction
 * Instead, it extends: gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction
 * The difference between them is in the AspectJ wormhole pattern used 
 * to inject the large icon when creating the action's com.nomagic.magicdraw.ui.states.SymbolDrawState.
 * 
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 * @see gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction
 * @see gov.nasa.jpl.magicdraw.advice.actions.paths.EnhancedDrawDynamicPathActionCreateState
 */
case class DrawDynamicPathAction
( finalizationAction: DynamicPathFinalizationAction,
  diagram: DiagramPresentationElement,
  ID: String, name: String, key: KeyStroke, largeIcon: Icon)
  extends EnhancedDrawPathAction(finalizationAction, diagram, ID, name, key, largeIcon) {
  
  override def createElement(): Element = 
    finalizationAction.creatorHelper.createElement(Project.getProject(diagram)) match {
    case Success(e) =>
      e
    case Failure(e) =>
      throw e
  }
  
  override def createPathElement(): PathElement = 
    createElement() match {
    case null =>
      null
    case e: Element =>
      finalizationAction.creatorHelper.createPathElement(e)
  }
  
  override def createAdditionalDrawAction(): AdditionalDrawAction =
    finalizationAction
  
  override def clone(): Object =
    copy()
}