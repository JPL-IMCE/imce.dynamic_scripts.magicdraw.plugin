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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes

import javax.swing.Icon
import javax.swing.KeyStroke
import scala.language.implicitConversions
import scala.language.postfixOps
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.actions.DrawShapeDiagramAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import scala.util.Success
import scala.util.Failure

/**
 * @BUG 
 * 
 * Report to NoMagic a problem with the fact that the icon of a DrawShapeDiagramAction does not always show. 
 * This affects MD's diagram toolbar buttons as well (e.g., Class, Package, etc...)
 */

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DrawDynamicShapeAction(
    finalizationAction: DynamicShapeFinalizationAction,
    diagram: DiagramPresentationElement,
    ID: String, name: String, key: KeyStroke, largeIcon: Icon)
    extends DrawShapeDiagramAction(ID, name, key) {
  
  setDiagram(diagram)
  setLargeIcon(largeIcon)
  
  override def createElement(): Element = 
    finalizationAction.creatorHelper.createElement(Project.getProject(diagram)) match {
    case Success(e) => e
    case Failure(e) => throw e
  } 
  
  override def createPresentationElement(): PresentationElement = {
    val pe = super.createPresentationElement()
    pe
  }
  
  override def getCustomAdditionalDrawAction(): AdditionalDrawAction = {
    finalizationAction
  }
  
  override def clone(): Object = {
    val c = copy()
    c
  }
}