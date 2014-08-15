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
import java.lang.reflect.Method
import java.net.MalformedURLException
import javax.swing.KeyStroke
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.utils.MDLog
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MainToolbarMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.MagicDrawValidationDataResults

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicScriptsLaunchToolbarMenuAction( action: MainToolbarMenuAction, id: String )
  extends NMAction( id, action.name.hname, null.asInstanceOf[KeyStroke] ) {

  override def actionPerformed( ev: ActionEvent ): Unit = {
    val log = MDLog.getPluginsLog()

    val previousTime = System.currentTimeMillis()

    val message = action.prettyPrint( "" )
    val guiLog = Application.getInstance().getGUILog()

    ClassLoaderHelper.createDynamicScriptClassLoader( action ) match {
      case Failure( ex ) =>
        val error = "${message}: project not found '${menuAction.projectName.jname}'"
        log.error( error )
        guiLog.showError( error )
        return

      case Success( scriptCL ) => {
        val localClassLoader = Thread.currentThread().getContextClassLoader()
        try {

          Thread.currentThread().setContextClassLoader( scriptCL )

          val c = scriptCL.loadClass( action.className.jname )
          if ( c == null ) {
            val error = "${message}: class '${menuAction.className.jname}' not found in project '${menuAction.projectName.jname}'"
            log.error( error )
            guiLog.showError( error )
            return
          }

          val m = lookupMethod( c, action ) match {
            case Failure( t ) =>
              val error = s"${message}: ${t.getMessage()}"
              log.error( error, t )
              guiLog.showError( error, t )
              return
            case Success( m ) => m
          }

          val r = m.invoke( null, action, ev )

          val currentTime = System.currentTimeMillis()
          log.info( s"${message} took ${currentTime - previousTime} ms" )

          r match {
            case Failure( ex ) =>
              val ex_message = message + s"\n${ex.getMessage()}"
              log.error( ex_message, ex )
              guiLog.showError( ex_message, ex )

            case Success( None ) =>
              ()

            case Success( Some( v: MagicDrawValidationDataResults ) ) => {

            }
            case _ =>
              ()
          }

        }
        catch {
          case ex: InvocationTargetException =>
            val t = ex.getTargetException() match { case null => ex; case t => t }
            val ex_message = message + s"\n${t.getMessage()}"
            log.error( ex_message, t )
            guiLog.showError( ex_message, t )
            return
          case ex @ ( _: ClassNotFoundException | _: SecurityException | _: NoSuchMethodException | _: IllegalArgumentException | _: IllegalAccessException | _: MalformedURLException | _: NoSuchMethodException ) =>
            val ex_message = message + s"\n${ex.getMessage()}"
            log.error( ex_message, ex )
            guiLog.showError( ex_message, ex )
            return
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }

  def lookupMethod( clazz: java.lang.Class[_], action: DynamicActionScript ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname, classOf[MainToolbarMenuAction], classOf[ActionEvent] ) match {
        case m: Method => Success( m )
        case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
      }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
    }

}