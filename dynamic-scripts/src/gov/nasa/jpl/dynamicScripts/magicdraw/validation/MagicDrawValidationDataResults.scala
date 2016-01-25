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
package gov.nasa.jpl.dynamicScripts.magicdraw.validation

import java.awt.event.ActionEvent

import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.annotation.{Annotation, AnnotationAction}
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.openapi.uml.{ReadOnlyElementException, SessionManager}
import com.nomagic.magicdraw.validation.{RuleViolationResult, ValidationRunData}
import com.nomagic.task.RunnableWithProgress
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import gov.nasa.jpl.dynamicScripts.magicdraw.validation

import scala.collection.JavaConversions._
import scala.collection.immutable._
import scala.language.{implicitConversions, postfixOps}
import scala.{Boolean, Option, None, Some, Unit}
import scala.Predef.String

/**
 * Convenient wrapper for MagicDraw validation results with a new capability for post-processing actions
  *
  * @see Validation chapter in MD Open API User Manual
 *
 * The MD Open API for creating validation annotations & actions requires references to
 * <<UML Standard Profile::Validation Profile::validationRule>>-stereotyped constraints defined in an
 * <<UML Standard Profile::Validation Profile::validationSuite>>-stereotyped package.
 *
 * The MD Open API manual does not have any concept of "post-processing action".
  * @example Java's tediousness {{{
 *
 * // Constructing a ValidationRunData object requires a reference to validation suite package.
 * // For example, given a set of MD Annotations: Set<Annotation> annotations,
 *
 * Application application = Application.getInstance();
 * Project project = application.getProject();
 * ValidationSuiteHelper vsh = ValidationSuiteHelper.getInstance(project);
 * String suiteQName = "...";
 * Package validationSuite = null;
 * 	for (Package suite : vsh.getValidationSuites()) {
 * 			if (suiteQName.equals(suite.getQualifiedName())) {
 * 				validationSuite = suite;
 * 				break;
 * 			}
 * 		}
 * 		if (null != validationSuite) {
 *    Collection<Constraint> validationConstraints = vsh.getValidationRules(validationSuite);
 *    ...
 *    EnumerationLiteral lowestLevel = ...;
 *    Set<Element> elements = ...;
 *    ...
 *    ValidationRunData runData = new ValidationRunData(validationSuite, false, elements, lowestLevel);
 *    ...
 *  }
 *
 * // Constructing a RuleViolationResult object requires a reference to a validation constraint;
 * // for example, in the context of the above:
 *
 * 		if (null != validationSuite) {
 *    Collection<Constraint> validationConstraints = vsh.getValidationRules(validationSuite);
 *    EnumerationLiteral lowestLevel = null;
 *    Set<Element> elements = new HashSet<Element>();
 *    List<RuleViolationResult> results = new ArrayList<RuleViolationResult>();
 *    for (Annotation annotation : annotations) {
 *      EnumerationLevel severity = annotation.getSeverity();
 *      if (lowestLevel == null || ValidationSuiteHelper.isSeverityHigherOrEqual(lowestLevel, severity)) {
 *         lowestLevel = severity;
 *      }
 *      elements.add((Element) annotation.getTarget());
 *      results.add(new RuleViolationResult(annotation, annotation.getConstraint()));
 *    }
 *    ValidationRunData runData = new ValidationRunData(validationSuite, false, elements, lowestLevel);
 *    List<RunnableWithProgress> postSessionActions = new ArrayList<RunnableWithProgress>();
 *    return new MagicDrawValidationDataResults("<title>", runData, results, postSessionActions);
 *  }
 * }}}
  * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class MagicDrawValidationDataResults(
  val title: String,
  val runData: ValidationRunData,
  val results: java.util.Collection[RuleViolationResult],
  val postSessionActions: java.util.Collection[RunnableWithProgress] )

object MagicDrawValidationDataResults {

  import validation.internal.MDValidationAPIHelper._

  /**
   * In the MD validation result window, multiple annotation actions can be invoked from selected annotations if:
   * - all of the annotation actions have the same NMAction.getID
   * - all the annotation actions implement the MD AnnotationAction interface trait
   */
  abstract class ValidationAnnotationAction
  ( ID: String,
    message: String,
    var annotation: Option[Annotation] = None )
    extends NMAction( ID, ID, 0 ) with AnnotationAction {

    setDescription( message )

    override def actionPerformed( e: ActionEvent ): Unit =
      annotation match {
        case Some( a ) => if ( canExecute( a ) ) execute( List( a ) ) else ()
        case None      => ()
      }

    override def canExecute
    ( annotations: java.util.Collection[Annotation] )
    : Boolean =
      ( null != annotations ) && ( annotations forall canExecute )

    override def execute
    ( annotations: java.util.Collection[Annotation] )
    : Unit =
      if ( null != annotations && !annotations.isEmpty ) {
        val sm = SessionManager.getInstance
        sm.createSession( getName )
        try {
          annotations foreach execute
          sm.closeSession()
        }
        catch {
          case _: ReadOnlyElementException =>
            sm.cancelSession()
          case _: java.lang.Throwable =>
            sm.cancelSession()
        }
      }

    def canExecute( annotation: Annotation ): Boolean

    def execute( annotation: Annotation ): Unit

    override def updateState: Unit =
      annotation match {
        case Some( a ) => enabled = canExecute( a )
        case None      => ()
      }

  }

  def makeAnnotationNodes4MagicDrawValidationDataResults
  ( project: Project,
    r: MagicDrawValidationDataResults )
  : Iterable[AnnotationNodeInfo] =
    r.results.to[Iterable] map { violation =>
      AnnotationNodeInfo(
        violation.getErrorMessage,
        ValidationAnnotation(
          project.getValidationSuiteOfResult( violation ),
          violation.getRule,
          violation.getAnnotation ) )
    }

  def addActionToList
  ( action: NMAction,
    actions: java.util.List[NMAction] )
  : java.util.List[_ <: NMAction] =
    if ( actions == null || actions.isEmpty )
      java.util.Collections.singletonList( action )
    else { actions.add( action ); actions }

  def addValidationAnnotationActionToAnnotation
  ( action: ValidationAnnotationAction, a: Annotation )
  : Annotation = {
    val _a = new Annotation(
      a.getTargetObject,
      a.getConstraint,
      a.getText,
      addActionToList( action, a.getActions.asInstanceOf[java.util.List[NMAction]] ) )
    action.annotation = Some( _a )
    _a
  }

  def addValidationAnnotationActionToFirstRuleViolationResultForElement
  ( action: ValidationAnnotationAction,
    e: Element,
    results: Iterable[RuleViolationResult] )
  : Iterable[RuleViolationResult] = {
    var added = false
    for ( result <- results )
      yield if ( e != result.getElement ) result
    else if ( added ) result
    else {
      added = true
      new RuleViolationResult(
        addValidationAnnotationActionToAnnotation( action, result.getAnnotation ), result.getRule )
    }
  }

  def addValidationAnnotationActionToMagicDrawValidationDataResultsException
  ( action: ValidationAnnotationAction,
    element: Element,
    t: MagicDrawValidationDataResultsException )
  : MagicDrawValidationDataResultsException =
    MagicDrawValidationDataResultsException(
      t.validationDataResults.copy(
        results = addValidationAnnotationActionToFirstRuleViolationResultForElement(
          action,
          element,
          t.validationDataResults.results.to[Iterable] ) ) )

}