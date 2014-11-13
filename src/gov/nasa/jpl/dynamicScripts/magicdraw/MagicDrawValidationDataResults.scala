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
package gov.nasa.jpl.dynamicScripts.magicdraw

import java.lang.reflect.InvocationTargetException

import scala.collection.JavaConversions.asJavaCollection
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.validation.RuleViolationResult
import com.nomagic.magicdraw.validation.ValidationRunData
import com.nomagic.magicdraw.validation.ValidationSuiteHelper
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager
import com.nomagic.task.ProgressStatus
import com.nomagic.task.RunnableWithProgress
import com.nomagic.ui.ProgressStatusRunner
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.utils.Utilities

/**
 * Convenient wrapper for MagicDraw validation results with a new capability for post-processing actions
 * @see Validation chapter in MD Open API User Manual
 * 
 * The MD Open API for creating validation annotations & actions requires references to
 * <<UML Standard Profile::Validation Profile::validationRule>>-stereotyped constraints defined in an
 * <<UML Standard Profile::Validation Profile::validationSuite>>-stereotyped package.
 *
 * The MD Open API manual does not have any concept of "post-processing action".
 *
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
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class MagicDrawValidationDataResults(
  val title: String,
  val runData: ValidationRunData,
  val results: java.util.Collection[RuleViolationResult],
  val postSessionActions: java.util.Collection[RunnableWithProgress] )

object MagicDrawValidationDataResults {

  def getValidationSuiteHelper( p: Project ): ValidationSuiteHelper =
    ValidationSuiteHelper.getInstance( p )

  def getValidationSeverityLevel( p: Project, severityLevel: String ): EnumerationLiteral =
    getValidationSuiteHelper( p ).getSeverityLevel( severityLevel )

  case class ValidationSuiteInfo( vsh: ValidationSuiteHelper, suite: Package ) {}

  def lookupValidationSuite( p: Project, suiteQualifiedName: String ): Option[ValidationSuiteInfo] = {
    val vsh = ValidationSuiteHelper.getInstance( p )
    vsh.getValidationSuites().find { s => s.getQualifiedName() == suiteQualifiedName } match {
      case None                   => None
      case Some( suite: Package ) => Some( ValidationSuiteInfo( vsh, suite ) )
    }
  }

  def lookupValidationConstraint( vSuiteInfo: ValidationSuiteInfo, constraintQualifiedName: String ): Option[Constraint] =
    vSuiteInfo.vsh.getValidationRules( vSuiteInfo.suite ) find { c => c.getQualifiedName() == constraintQualifiedName }

  def getMDValidationProfileAndConstraint( p: Project, validationSuiteQName: String, validationConstraintQName: String ): Option[( ValidationSuiteInfo, Constraint )] =
    lookupValidationSuite( p, validationSuiteQName ) match {
      case None => None
      case Some( vSuite ) =>
        lookupValidationConstraint( vSuite, validationConstraintQName ) match {
          case None      => None
          case Some( c ) => Some( ( vSuite, c ) )
        }
    }

  val mdValidationProfileQName = "UML Standard Profile::Validation Profile::Composition Integrity"
  val mdValidationConstraintQName = "UML Standard Profile::Validation Profile::Composition Integrity::Illegal Reference"

  /** Creates a `MagicDrawValidationDataResultsException` for elements, optionally with validation annotation actions
   * 
   * @param p the active MagicDraw project
   * @param validationMessage to be shown as the title of MagicDraw's Validation results window
   * @param elementMessages maps elements to a pair of a validation message and validation annotation actions
   * @param validationSuiteQName the qualified name of a MagicDraw validation suite profile, defaults to [[mdValidationProfileQName]]
   * @param validationConstraintQName the qualified name of a MagicDraw validation constraint, defaults to [[mdValidationConstraintQName]]
   */
  def makeMDIllegalArgumentExceptionValidation(
    p: Project,
    validationMessage: String,
    elementMessages: Map[Element, ( String, List[NMAction] )],
    validationSuiteQName: String = mdValidationProfileQName,
    validationConstraintQName: String = mdValidationConstraintQName ): MagicDrawValidationDataResultsException =
    getMDValidationProfileAndConstraint( p, validationSuiteQName, validationConstraintQName ) match {
      case None =>
        throw new IllegalArgumentException( s"Failed to find MD's Validation Profile '${validationSuiteQName}' & Constraint '${validationConstraintQName}'" )
      case Some( ( vSuite, c ) ) =>
        val runData = new ValidationRunData( vSuite.suite, false, elementMessages.keys, vSuite.vsh.getRuleSeverityLevel( c ) )
        val results = elementMessages map {
          case ( element, ( message, actions ) ) =>
            new RuleViolationResult( new Annotation( element, c, message, actions ), c )
        }
        MagicDrawValidationDataResultsException( MagicDrawValidationDataResults( validationMessage, runData, results, List[RunnableWithProgress]() ) )
    }

  def showMDValidationDataResultsIfAny( data: Option[MagicDrawValidationDataResults] ): Unit =
    data match {
      case None      => ()
      case Some( d ) => showMDValidationDataResults( d )
    }

  def showMDValidationDataResultsAndExecutePostSessionActions( p: Project, sm: SessionManager, r: MagicDrawValidationDataResults, message: String ): Try[Unit] =
    try {
      if ( p != null && sm.isSessionCreated( p ) ) {
        sm.closeSession( p )
      }
      MagicDrawValidationDataResults.showMDValidationDataResults( r )
      Success( Unit )
    }
    finally {
      MagicDrawValidationDataResults.doPostSessionActions( p, message, r ) match {
        case Success( _ ) =>
          Success( Unit )
        case Failure( t ) =>
          Failure( t )
      }
    }

  def showMDValidationDataResults( d: MagicDrawValidationDataResults ): Unit =
    if ( d.results.nonEmpty )
      Utilities.invokeAndWaitOnDispatcher( new Runnable() {
        override def run: Unit =
          ValidationResultsWindowManager.updateValidationResultsWindow(
            d.title + System.currentTimeMillis().toString,
            d.title,
            d.runData,
            d.results )
      } )

  def doPostSessionActions( project: Project, message: String, data: MagicDrawValidationDataResults ): Try[Unit] = {
    if ( data.postSessionActions.isEmpty() )
      return Success( Unit )

    var result: Try[Unit] = Success( Unit )

    ProgressStatusRunner.runWithProgressStatus( new RunnableWithProgress() {
      override def run( progressStatus: ProgressStatus ): Unit = {
        val sm = SessionManager.getInstance
        try {
          progressStatus.setIndeterminate( true )
          for ( request <- data.postSessionActions ) {
            if ( progressStatus.isCancel() ) {
              if ( sm.isSessionCreated() )
                sm.cancelSession()
              return
            }

            // With direct invocation, i.e.:
            //
            //   request.doAction(progressStatus);
            //
            // we would need an UncaughtExceptionHandler to detect any problem that may have occured.

            // With reflective invocation, we will get an InvocationTargetException instead.
            request.getClass().getMethod( "doAction", classOf[ProgressStatus] ) match {
              case null => ()
              case m    => m.invoke( request, progressStatus )
            }

          }
        }
        catch {
          case ex: InvocationTargetException =>
            if ( project != null && sm.isSessionCreated( project ) )
              sm.cancelSession( project )
            ex.getTargetException() match {
              case t: Throwable => result = Failure( t )
              case null         => result = Failure( ex )
            }

          case e @ ( _: InvocationTargetException | _: SecurityException | _: IllegalArgumentException | _: IllegalAccessException | _: NoSuchMethodException ) =>
            if ( project != null && sm.isSessionCreated( project ) )
              sm.cancelSession( project )
            result = Failure( e )
        }
      }
    }, message, true, 0 )

    result
  }
}
