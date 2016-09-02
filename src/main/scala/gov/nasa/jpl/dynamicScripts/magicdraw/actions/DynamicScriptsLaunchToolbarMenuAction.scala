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

import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MainToolbarMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper.ResolvedClassAndMethod

import scala.util.{Failure, Success}
import scala.Unit
import scala.Predef.{classOf, String}
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicScriptsLaunchToolbarMenuAction
( action: MainToolbarMenuAction, id: String )
  extends NMAction( id, action.name.hname, null.asInstanceOf[KeyStroke] ) {

  override def getDescription: String =
    action.prettyPrint("  ")
    
  override def actionPerformed( ev: ActionEvent ): Unit = {
    val previousTime = System.currentTimeMillis()
    val message = action.prettyPrint( "" ) + "\n"

    ClassLoaderHelper.createDynamicScriptClassLoader( action ) match {
      case Failure( t ) =>
        ClassLoaderHelper.reportError( action, message, t )

      case Success( scriptCL: URLClassLoader ) => {
        val localClassLoader = Thread.currentThread().getContextClassLoader
        Thread.currentThread().setContextClassLoader( scriptCL )

        try {
          ClassLoaderHelper.lookupClassAndMethod( scriptCL, action,
            classOf[Project], classOf[ActionEvent], classOf[MainToolbarMenuAction] ) match {
              case Failure( t ) =>
                ClassLoaderHelper.reportError( action, message, t )

              case Success( cm: ResolvedClassAndMethod ) =>
                ClassLoaderHelper.ignoreResultOrthrowFailure(
                  ClassLoaderHelper
                    .invokeAndReport( previousTime, Application.getInstance().getProject, ev, cm )
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