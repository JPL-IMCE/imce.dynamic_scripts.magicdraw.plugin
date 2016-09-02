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

package gov.nasa.jpl.dynamicScripts.magicdraw.actions

import java.awt.event.ActionEvent
import java.lang.{System, Thread}
import java.net.URLClassLoader

import javax.swing.KeyStroke

import scala.util.Failure
import scala.util.Success

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.actions.DefaultDiagramAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DiagramContextMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper.ResolvedClassAndMethod
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

import scala.{StringContext, Unit}
import scala.Predef.{classOf, String}
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicDiagramContextMenuActionForDiagram
( project: Project,
  diagram: DiagramPresentationElement,
  menuAction: DiagramContextMenuAction,
  key: KeyStroke,
  group: String )
  extends DefaultDiagramAction( menuAction.name.hname, menuAction.name.hname, key, group ) {

  override def toString: String =
    s"${menuAction.name.hname}"

  override def getDescription: String =
    menuAction.prettyPrint("  ")
    
  override def updateState(): Unit = {
    super.updateState()
    val isEnabled =
      ClassLoaderHelper.isDynamicActionScriptAvailable( menuAction ) &&
      MDUML.isAccessCompatibleWithElements( menuAction.access, diagram )
    setEnabled(isEnabled)
  }

  override def actionPerformed( ev: ActionEvent ): Unit = {
    val previousTime = System.currentTimeMillis()
    val message = menuAction.prettyPrint( "" ) + "\n"

    ClassLoaderHelper.createDynamicScriptClassLoader( menuAction ) match {
      case Failure( t ) =>
        ClassLoaderHelper.reportError( menuAction, message, t )

      case Success( scriptCL: URLClassLoader ) => {
        val localClassLoader = Thread.currentThread().getContextClassLoader
        Thread.currentThread().setContextClassLoader( scriptCL )

        try {
          ClassLoaderHelper.lookupClassAndMethod(
            scriptCL, menuAction,
            classOf[Project], classOf[ActionEvent],
            classOf[DiagramContextMenuAction],
            classOf[DiagramPresentationElement] ) match {
            case Failure( t ) =>
              ClassLoaderHelper.reportError( menuAction, message, t )

            case Success( cm: ResolvedClassAndMethod ) =>
              ClassLoaderHelper.ignoreResultOrthrowFailure(
                ClassLoaderHelper
                  .invokeAndReport( previousTime, project, ev, cm, diagram )
              )
          }
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }
}