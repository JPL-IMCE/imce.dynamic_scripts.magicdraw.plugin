/**
 * License Terms
 *
 * Copyright (c) 2014, California
 * Institute of Technology ("Caltech").  U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *
 *  *   Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *  *   Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the
 *      distribution.
 *
 *  *   Neither the name of Caltech nor its operating division, the Jet
 *      Propulsion Laboratory, nor the names of its contributors may be
 *      used to endorse or promote products derived from this software
 *      without specific prior written permission.
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
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException
import java.util.Collection

import javax.swing.KeyStroke

import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.ui.actions.DefaultDiagramAction
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.utils.Utilities

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.MagicDrawValidationDataResults

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicDiagramContextMenuActionForMultipleSelection(
  project: Project, selected: List[PresentationElement],
  menuAction: DiagramContextMenuAction,
  key: KeyStroke,
  group: String ) extends DefaultDiagramAction( menuAction.name.hname, menuAction.name.hname, key, group ) {

  override def toString(): String = 
    s"${menuAction.name.hname}"

  override def actionPerformed( ev: ActionEvent ): Unit = {
    val previousTime = System.currentTimeMillis()
    val log = MDLog.getPluginsLog()
    val guiLog = Application.getInstance().getGUILog()
    val p = DynamicScriptsPlugin.getInstance()
    val sm = SessionManager.getInstance()

    val message = menuAction.prettyPrint("")
    
    val localClassLoader = Thread.currentThread().getContextClassLoader()
    try {
      val scriptCL = ClassLoaderHelper.createDynamicScriptClassLoader( menuAction )
      Thread.currentThread().setContextClassLoader( scriptCL )

      val c = scriptCL.loadClass( menuAction.className.jname )
      if ( c == null ) {
        val error = "${message}: class '${menuAction.className.jname}' not found in project '${menuAction.projectName.jname}'"
        log.error( error )
        guiLog.showError( error )
        return
      }

      val m = c.getMethod( menuAction.methodName.sname, classOf[java.util.Collection[PresentationElement]] )
      if ( m == null ) {
        val error = "${message}: method '${menuAction.methodName.sname}(Collection<PresentationElement>)' not found in project/class '${menuAction.projectName.jname}/${action.className.jname}'"
        log.error( error )
        guiLog.showError( error )
        return
      }

      val r = m.invoke( null, selected )

      val currentTime = System.currentTimeMillis()
      log.info( s"${message} took ${currentTime - previousTime} ms" )

      r match {
        case Some(MagicDrawValidationDataResults(title, runData, results)) => 
          Utilities.invokeAndWaitOnDispatcher(new Runnable() {
            override def run(): Unit = {
              ValidationResultsWindowManager.updateValidationResultsWindow(currentTime.toString(), title, runData, results)
            }
          })
        case _ => 
          ()
      }

    }
    catch {
      case ex: InvocationTargetException =>
        val t = ex.getTargetException() match { case null => ex; case t => t }
        log.error( message, t )
        guiLog.showError( message, t )
      case ex @ ( _: ClassNotFoundException | _: SecurityException | _: NoSuchMethodException | _: IllegalArgumentException | _: IllegalAccessException | _: MalformedURLException | _: NoSuchMethodException ) =>
        log.error( message, ex )
        guiLog.showError( message, ex )
    }
    finally {
      Thread.currentThread().setContextClassLoader( localClassLoader )
    }
  }

}