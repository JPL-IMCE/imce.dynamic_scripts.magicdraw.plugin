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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions

import java.awt.event.ActionEvent
import java.net.URLClassLoader

import javax.swing.KeyStroke

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Success

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.actions.DefaultDiagramAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DiagramContextMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper.ResolvedClassAndMethod
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicDiagramContextMenuActionForMultipleSelection(
  project: Project, 
  diagram: DiagramPresentationElement,
  selected: java.util.Collection[PresentationElement],
  menuAction: DiagramContextMenuAction,
  key: KeyStroke,
  group: String ) extends DefaultDiagramAction( menuAction.name.hname, menuAction.name.hname, key, group ) {

  override def toString(): String =
    s"${menuAction.name.hname}"

  override def getDescription(): String =
    menuAction.prettyPrint("  ")
    
  override def updateState(): Unit = {
    super.updateState()
    setEnabled( ClassLoaderHelper.isDynamicActionScriptAvailable( menuAction ) && MDUML.isAccessCompatibleWithElements( menuAction.access, ( diagram :: selected.toList) : _*))
  }
  
  override def actionPerformed( ev: ActionEvent ): Unit = {
    val previousTime = System.currentTimeMillis()
    val message = menuAction.prettyPrint( "" ) + "\n"

    ClassLoaderHelper.createDynamicScriptClassLoader( menuAction ) match {
      case Failure( t ) =>
        ClassLoaderHelper.reportError( menuAction, message, t )
        return

      case Success( scriptCL: URLClassLoader ) => {
        val localClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader( scriptCL )

        try {
          ClassLoaderHelper.lookupClassAndMethod( scriptCL, menuAction, 
              classOf[Project], classOf[ActionEvent], classOf[DiagramContextMenuAction], classOf[DiagramPresentationElement], classOf[java.util.Collection[PresentationElement]] ) match {
            case Failure( t ) =>
              ClassLoaderHelper.reportError( menuAction, message, t )
              return

            case Success( cm: ResolvedClassAndMethod ) =>
              ClassLoaderHelper.invokeAndReport( previousTime, project, ev, cm, diagram, selected )
          }
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }
}