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

package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.internal

import java.awt.Point
import java.lang.{ClassNotFoundException, IllegalAccessException, IllegalArgumentException}
import java.lang.{NoSuchMethodException, Object, Runnable, SecurityException, System, Thread}
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException
import javax.swing.Icon
import javax.swing.KeyStroke

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.symbols.{PresentationElement, DiagramPresentationElement}
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.magicdraw.uml.symbols.paths.PathElement
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.utils.Utilities

import gov.nasa.jpl.dynamicScripts.magicdraw._
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathCreatorHelper
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelPathInstanceCreator
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.UncaughtExceptionHandler
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.MagicDrawValidationDataResults
import gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction

import scala.util.Failure
import scala.util.Success
import scala.{Boolean, None, Some, StringContext, Unit}
import scala.Predef.String

@scala.deprecated("", "")
class DrawPathDiagramActionHelper {}

object DrawPathDiagramActionHelper {

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


  case class DynamicPathFinalizationAction
  ( action: ToplevelPathInstanceCreator,
    creatorHelper: DynamicPathCreatorHelper )
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
          val error = s"$message: project not found '${action.context}'"
          log.error( error )
          guiLog.showError( error )
          false

        case Success( scriptCL ) => {
          val localClassLoader = Thread.currentThread().getContextClassLoader
          try {

            Thread.currentThread().setContextClassLoader( scriptCL )

            val c = scriptCL.loadClass( action.className.jname )
            if ( c == null ) {

              val error = s"$message: class '${action.className.jname}' not found in project '${action.context}'"
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

              val r: java.lang.Object = creatorHelper.invokeMethod(m, action, pe, point, e)

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
                        ValidationResultsWindowManager.updateValidationResultsWindow(
                          currentTime.toString, title, runData, results)
                      }
                    })
                  if (!postSessionActions.isEmpty)
                    guiLog.showError(
                      s"There are ${postSessionActions.size()} post-session actions that will not be executed " +
                        "because session management is not accessible for MD shape finalization actions")
                  false

                case b: java.lang.Boolean =>
                  b.booleanValue()

                case _ =>
                  false
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
          }
          finally {
            Thread.currentThread().setContextClassLoader( localClassLoader )
          }
        }
      }
    }

    override def afterExecute( pe: PresentationElement, point: Point ): Unit = {}

  }
}