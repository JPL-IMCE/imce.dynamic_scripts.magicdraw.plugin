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
import scala.{Boolean, Option, None, Some, Unit}
import scala.Predef.String

/**
  * Convenience wrapper for MagicDraw validation results with a new capability for post-processing actions
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
  *
  * @param title Title for the MagicDraw validation results window
  * @param runData A MagicDraw ValidationRunData
  * @param results This must be a mutable Java collection of RuleViolationResult
  * @param postSessionActions
  */
case class MagicDrawValidationDataResults(
  title: String,
  runData: ValidationRunData,
  results: java.util.Collection[RuleViolationResult],
  postSessionActions: java.util.Collection[RunnableWithProgress] )

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