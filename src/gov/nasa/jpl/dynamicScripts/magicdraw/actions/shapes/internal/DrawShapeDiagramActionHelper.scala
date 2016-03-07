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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.internal

import java.awt.Point
import java.lang.{ClassNotFoundException, IllegalAccessException, IllegalArgumentException}
import java.lang.{Object, NoSuchMethodException, Runnable, SecurityException, System, Thread}
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException
import javax.swing.Icon
import javax.swing.KeyStroke

import com.nomagic.magicdraw.core.{Application, Project}
import com.nomagic.magicdraw.ui.actions.DrawShapeDiagramAction
import com.nomagic.magicdraw.uml.symbols.{DiagramPresentationElement, PresentationElement}
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.utils.Utilities

import gov.nasa.jpl.dynamicScripts.magicdraw._
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelShapeInstanceCreator
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.MagicDrawValidationDataResults
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeCreatorHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.UncaughtExceptionHandler

import scala.language.implicitConversions
import scala.util.Failure
import scala.util.Success
import scala.{Boolean, None, Some, StringContext, Unit}
import scala.Predef.String

@scala.deprecated("", "")
class DrawShapeDiagramActionHelper {}

object DrawShapeDiagramActionHelper {

  /**
    * BUG
    *
    * Report to NoMagic a problem with the fact that the icon of a DrawShapeDiagramAction does not always show.
    * This affects MD's diagram toolbar buttons as well (e.g., Class, Package, etc...)
    */
  case class DrawDynamicShapeAction
  ( finalizationAction: DynamicShapeFinalizationAction,
    diagram: DiagramPresentationElement,
    ID: String, name: String, key: KeyStroke, largeIcon: Icon)
    extends DrawShapeDiagramAction(ID, name, key) {

    setDiagram(diagram)
    setLargeIcon(largeIcon)

    override def createElement(): Element =
      finalizationAction.creatorHelper.createElement(Project.getProject(diagram)) match {
        case Success(e) =>
          e
        case Failure(e) =>
          throw e
      }

    override def createPresentationElement(): PresentationElement = {
      val pe = super.createPresentationElement()
      pe
    }

    override def getCustomAdditionalDrawAction: AdditionalDrawAction = {
      finalizationAction
    }

    override def clone(): Object = {
      val c = copy()
      c
    }
  }


  case class DynamicShapeFinalizationAction
  ( action: ToplevelShapeInstanceCreator,
    creatorHelper: DynamicShapeCreatorHelper )
    extends AdditionalDrawAction {

    def getSortKey: String = action.sortKey()

    def isEnabled: Boolean =
      ClassLoaderHelper.isDynamicActionScriptAvailable( action ) && creatorHelper.isResolved

    override def execute( pe: PresentationElement, point: Point ): Boolean = {
      import MDGUILogHelper._
      val guiLog = getGUILog
      val log = guiLog.getMDPluginsLog

      val e = pe.getElement

      val previousTime = System.currentTimeMillis()

      val message = action.prettyPrint( "" )

      UncaughtExceptionHandler( message )
      ClassLoaderHelper.createDynamicScriptClassLoader( action ) match {
        case Failure( ex ) =>
          val error = s"$message: project context not found '${action.context.prettyPrint("")}'"
          log.error( error )
          guiLog.showError( error )
          false

        case Success( scriptCL ) => {
          val localClassLoader = Thread.currentThread().getContextClassLoader
          try {

            Thread.currentThread().setContextClassLoader( scriptCL )

            val c = scriptCL.loadClass( action.className.jname )
            if ( c == null ) {

              val error =
                s"$message: class '${action.className.jname}' not found "+
                s"in project '${action.context.prettyPrint("")}'"
              log.error( error )
              guiLog.showError( error )
              false

            } else {

              val m = creatorHelper.lookupMethod(c, action) match {
                case Failure(t) =>
                  val error = s"$message: ${t.getMessage}"
                  log.error(error, t)
                  guiLog.showError(error, t)
                  return false
                case Success(m) =>
                  m
              }

              val r = creatorHelper.invokeMethod(m, action, pe, point, e)

              val currentTime = System.currentTimeMillis()
              log.info(s"$message took ${prettyDurationFromTo(previousTime, currentTime)}")

              r match {
                case Failure(ex) =>
                  val ex_message = message + s"\n${ex.getMessage}"
                  log.error(ex_message, ex)
                  guiLog.showError(ex_message, ex)
                  false

                case Success(None) =>
                  true

                case Success(Some(MagicDrawValidationDataResults(title, runData, results, postSessionActions))) =>
                  if (!results.isEmpty)
                    Utilities.invokeAndWaitOnDispatcher(new Runnable() {
                      override def run(): Unit = {
                        ValidationResultsWindowManager
                          .updateValidationResultsWindow(currentTime.toString, title, runData, results)
                      }
                    })
                  if (!postSessionActions.isEmpty)
                    guiLog.showError(
                      s"There are ${postSessionActions.size()} post-session actions " +
                        s"that will not be executed because session management is not accessible " +
                        s"for MD shape finalization actions")
                  false

                case b: java.lang.Boolean => b.booleanValue()

                case _ => false
              }
            }

          } catch {
            case ex: InvocationTargetException =>
              val t = ex.getTargetException match { case null => ex; case t => t }
              val ex_message = message + s"\n${t.getMessage}"
              log.error( ex_message, t )
              guiLog.showError( ex_message, t )
              false

            case ex @ ( _: ClassNotFoundException | _: SecurityException | _: NoSuchMethodException |
                        _: IllegalArgumentException | _: IllegalAccessException | _: MalformedURLException |
                        _: NoSuchMethodException ) =>
              val ex_message = message + s"\n${ex.getMessage}"
              log.error( ex_message, ex )
              guiLog.showError( ex_message, ex )
              false

          } finally {
            Thread.currentThread().setContextClassLoader( localClassLoader )
          }
        }
      }
    }

    override def afterExecute( pe: PresentationElement, point: Point ): Unit = {}

  }
}