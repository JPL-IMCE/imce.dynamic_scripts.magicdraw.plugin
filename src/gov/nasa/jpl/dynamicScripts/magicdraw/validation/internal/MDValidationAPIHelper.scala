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
package gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal

import java.awt.event.ActionEvent
import java.lang.reflect.InvocationTargetException
import java.lang.{IllegalAccessException, IllegalArgumentException, NoSuchMethodException, Runnable}
import java.lang.{SecurityException, System, Throwable}

import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.AbstractAction
import javax.swing.JOptionPane

import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.ui.MagicDrawProgressStatusRunner
import com.nomagic.magicdraw.validation.RuleViolationResult
import com.nomagic.magicdraw.validation.ValidationRunData
import com.nomagic.magicdraw.validation.ValidationSuiteHelper
import com.nomagic.magicdraw.validation.ui.ValidationResultPanel
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager
import com.nomagic.task.ProgressStatus
import com.nomagic.task.RunnableWithProgress
import com.nomagic.ui.ProgressStatusRunner
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.utils.Utilities

import gov.nasa.jpl.dynamicScripts.magicdraw.wildCardMatch
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.{MagicDrawValidationDataResultsException, MagicDrawValidationDataResults}

import scala.collection.immutable._
import scala.collection.JavaConversions.asJavaCollection
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.{AnyVal, Boolean, Option, None, Ordering, Some, StringContext, Unit}
import scala.Predef.{classOf, require, String}

@scala.deprecated("", "")
class MDValidationAPIHelper(val p: Project) extends AnyVal {

  def getValidationSuiteHelper
  : ValidationSuiteHelper =
    ValidationSuiteHelper.getInstance(p)

  def getValidationSeverityLevel
  (severityLevel: String)
  : EnumerationLiteral =
    getValidationSuiteHelper.getSeverityLevel(severityLevel)

  def isValidationSeverityHigherOrEqual( level1: EnumerationLiteral, level2: EnumerationLiteral ): Boolean =
    ValidationSuiteHelper.isSeverityHigherOrEqual( level1, level2 )

  def lookupValidationSuite
  (suiteQualifiedName: String)
  : Option[Package] =
    getValidationSuiteHelper.getValidationSuites.find { s =>
      wildCardMatch(s.getQualifiedName, suiteQualifiedName)
    }

  def lookupValidationConstraint
  (vSuite: Package, constraintQualifiedName: String)
  : Option[Constraint] =
    getValidationSuiteHelper.getValidationRules(vSuite) find { c =>
      wildCardMatch(c.getQualifiedName, constraintQualifiedName)
    }

  def getValidationSuiteOfResult
  (r: RuleViolationResult)
  : Package = {
    val c = r.getRule
    getValidationSuiteHelper.getValidationSuites.find { p =>
      getValidationSuiteHelper.getValidationRules(p) contains c
    } match {
      case None =>
        throw new IllegalArgumentException("...")
      case Some(p) =>
        p
    }
  }

  def getRuleRawMessage
  ( c: Constraint )
  : Option[String] =
    getValidationSuiteHelper.getRuleRawMessage(c) match {
      case null => None
      case "" => None
      case s => Some(s)
    }

  def getRuleSeverityLevel
  ( c: Constraint )
  : Option[EnumerationLiteral] =
    Option(getValidationSuiteHelper.getRuleSeverityLevel(c))

  def updateValidationResultsWindow
  (title: String,
   d: MagicDrawValidationDataResults)
  : Unit = {
    ValidationResultsWindowManager.updateValidationResultsWindow(
      title,
      d.title,
      d.runData,
      d.results )
  }

  /**
    * MD's multiple annotation action invocation mechanism is limited to all annotation actions having the same ID.
    * In some cases, it would be convenient to select all annotations and
    * invoke all of the annotation actions regardless of whether they have the same ID or not.
    * This method adds a keyboard mapping to the MD validation results window (Control + Meta + A)
    * that will invoke all selected annotation actions (regardless of whether they have the same ID or not)
    */
  def showMDValidationDataResults
  ( d: MagicDrawValidationDataResults )
  : Unit =
    if ( d.results.nonEmpty )
      Utilities.invokeAndWaitOnDispatcher( new Runnable() {
        override def run: Unit = {
          val windowTitle = d.title + System.currentTimeMillis().toString
          updateValidationResultsWindow(windowTitle,d)

          val validationWindow = p.getWindow( windowTitle )
          val validationComponent = validationWindow.getContent.getWindowComponent
          validationComponent match {
            case null =>
              System.out.println( s"*** ValidationWindow -- no validation component!" )

            case validationPanel: ValidationResultPanel =>
              val table = validationPanel.getValidationResultTreeTable

              val inputMap = table.getTable.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
              val controlMetaA = KeyStroke.getKeyStroke( "control meta A" )
              require( controlMetaA != null )
              inputMap.put( controlMetaA, "ExecuteSelectedDynamicValidationAnnotationActions" )

              table.getTable.getActionMap.put(
                "ExecuteSelectedDynamicValidationAnnotationActions",
                new AbstractAction() {

                  override def actionPerformed( e: ActionEvent ): Unit = {

                    val ruleViolationResults = table.getSelectedObjects flatMap ( _ match {
                      case rvr: RuleViolationResult => Some( rvr )
                      case _                        => None
                    } )
                    val annotations = ruleViolationResults flatMap { r => Option.apply( r.getAnnotation ) }
                    val actions = annotations flatMap ( _.getActions match {
                      case null => Nil
                      case as   => as.toList
                    } )
                    val actionsByID = actions groupBy ( _.getID )

                    val messageSummary =
                      s"Ok to execute all ${actions.size} selected "+
                        s"dynamic validation annotion actions?\nSummary of actions by ID:\n"

                    val messageDetails = ( actionsByID map {
                      case ( id, actions ) =>
                        s" - ${actions.size} $id "
                    } ) mkString "\n"

                    val status = JOptionPane.showConfirmDialog(
                      Application.getInstance.getMainFrame,
                      messageSummary + messageDetails,
                      "Dynamic Validation Annotation Actions",
                      JOptionPane.OK_CANCEL_OPTION )

                    if ( status == JOptionPane.OK_OPTION ) {
                      val runnable = new RunnableWithProgress() {

                        def run( progressStatus: ProgressStatus ): Unit = {
                          progressStatus.setCurrent( 0 )
                          progressStatus.setMax( 0 )
                          progressStatus.setMax( actions.size.toLong )

                          actions foreach { action =>
                            if (progressStatus.isCancel)
                              return
                            progressStatus.setDescription(
                              s"Executing ${actions.size - progressStatus.getCurrent}"+
                                s"dynamic validation annotation actions...")
                            action.actionPerformed( null )
                            progressStatus.increase()
                          }
                        }
                      }

                      MagicDrawProgressStatusRunner.runWithProgressStatus(
                        runnable,
                        s"Executing ${actions.size} dynamic validation annotation actions",
                        true, 0 )
                    }
                  }
                } )

            case x =>
              System.out.println( s"*** ValidationWindow: ${x.getClass.getName}: $x" )
          }

        }
      } )

  def doPostSessionActions
  ( message: String, data: MagicDrawValidationDataResults )
  : Try[Unit] = {
    if (data.postSessionActions.isEmpty) {
      Success(())
    } else {
      var result: Try[Unit] = Success(())

      ProgressStatusRunner.runWithProgressStatus(new RunnableWithProgress() {
        override def run(progressStatus: ProgressStatus): Unit = {
          val sm = SessionManager.getInstance
          try {
            progressStatus.setIndeterminate(true)
            for (request <- data.postSessionActions) {
              if (progressStatus.isCancel()) {
                if (sm.isSessionCreated(p))
                  sm.cancelSession(p)
                return
              }

              // With direct invocation, i.e.:
              //
              //   request.doAction(progressStatus);
              //
              // we would need an UncaughtExceptionHandler to detect any problem that may have occured.

              // With reflective invocation, we will get an InvocationTargetException instead.
              request.getClass().getMethod("doAction", classOf[ProgressStatus]) match {
                case null => ()
                case m => m.invoke(request, progressStatus)
              }
              if (sm.isSessionCreated(p))
                sm.closeSession(p)
            }
          }
          catch {
            case ex: InvocationTargetException =>
              if (sm.isSessionCreated(p))
                sm.cancelSession(p)
              ex.getTargetException match {
                case t: Throwable => result = Failure(t)
                case null => result = Failure(ex)
              }

            case e@(_: InvocationTargetException | _: SecurityException |
                    _: IllegalArgumentException | _: IllegalAccessException |
                    _: NoSuchMethodException) =>
              if (sm.isSessionCreated(p))
                sm.cancelSession(p)
              result = Failure(e)
          }
        }
      }, message, true, 0)

      result
    }
  }

  def showMDValidationDataResultsAndExecutePostSessionActions
  ( sm: SessionManager, r: MagicDrawValidationDataResults, message: String )
  : Try[Unit] =
    try {
      if (sm.isSessionCreated(p)) {
        sm.closeSession(p)
      }
      showMDValidationDataResults(r)
      Success(())
    }
    finally {
      doPostSessionActions(message, r) match {
        case Failure(t) =>
          return Failure(t)
        case _ =>
          ()
      }
    }

  def showMDValidationDataResultsIfAny
  ( data: Option[MagicDrawValidationDataResults] )
  : Unit =
    data match {
      case None      =>
        ()
      case Some( d ) =>
        showMDValidationDataResults( d )
    }

  def getMDValidationProfileAndConstraint
  ( validationSuiteQName: String, validationConstraintQName: String )
  : Option[( Package, Constraint )] =
    lookupValidationSuite( validationSuiteQName ) match {
      case None =>
        None
      case Some( vSuite ) =>
        lookupValidationConstraint( vSuite, validationConstraintQName ) match {
          case None      =>
            None
          case Some( c ) =>
            Some( ( vSuite, c ) )
        }
    }

  /**
    * Creates a `MagicDrawValidationDataResultsException` for elements, optionally with validation annotation actions
    *
    * @param p the active MagicDraw project
    * @param validationMessage to be shown as the title of MagicDraw's Validation results window
    * @param elementMessages maps elements to a pair of a validation message and validation annotation actions
    * @param validationSuiteQName the qualified name of a MagicDraw validation suite profile,
    *                             defaults to [[MDValidationAPIHelper.mdValidationProfileQName]]
    * @param validationConstraintQName the qualified name of a MagicDraw validation constraint,
    *                                  defaults to [[MDValidationAPIHelper.mdValidationConstraintQName]]
    */
  def makeMDIllegalArgumentExceptionValidation
  ( validationMessage: String,
    elementMessages: Map[Element, ( String, List[NMAction] )],
    validationSuiteQName: String = MDValidationAPIHelper.mdValidationProfileQName,
    validationConstraintQName: String = MDValidationAPIHelper.mdValidationConstraintQName )
  : MagicDrawValidationDataResultsException =
    getMDValidationProfileAndConstraint( validationSuiteQName, validationConstraintQName ) match {
      case None =>
        throw new IllegalArgumentException(
          s"Failed to find MD's Validation Profile "+
            s"'$validationSuiteQName' & Constraint '$validationConstraintQName'" )
      case Some( ( vSuite, c ) ) =>
        getRuleSeverityLevel( c ).fold[MagicDrawValidationDataResultsException](
          throw new IllegalArgumentException(
            s"Failed to find MD's Validation Security Level "+
              s"'$validationSuiteQName' & Constraint '$validationConstraintQName'" )
        ) { level =>
          val runData = new ValidationRunData(vSuite, false, elementMessages.keys, level)
          val results =
            new java.util.ArrayList[RuleViolationResult](
              elementMessages
                .map {
                  case (element, (message, actions)) =>
                    new RuleViolationResult(new Annotation(element, c, message, actions), c)
                })
          MagicDrawValidationDataResultsException(
            MagicDrawValidationDataResults(
              validationMessage,
              runData,
              results,
              List[RunnableWithProgress]()))
        }
    }
}

object MDValidationAPIHelper {

  implicit def toMDValidationAPIHelper(p: Project): MDValidationAPIHelper =
    new MDValidationAPIHelper(p)

  val mdValidationProfileQName =
    "UML Standard Profile::Validation Profile::Composition Integrity"

  val mdValidationConstraintQName =
    "UML Standard Profile::Validation Profile::Composition Integrity::Illegal Reference"


  val SEVERITY_LEVEL_ORDERING = new Ordering[EnumerationLiteral]() {

    override def compare( level1: EnumerationLiteral, level2: EnumerationLiteral ) =
      if ( level1.equals( level2 ) ) 0
      else if ( ValidationSuiteHelper.isSeverityHigherOrEqual( level1, level2 ) ) -1
      else 1

  }
}