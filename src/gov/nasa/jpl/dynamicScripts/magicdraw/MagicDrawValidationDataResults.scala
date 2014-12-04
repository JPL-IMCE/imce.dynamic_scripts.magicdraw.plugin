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

import java.awt.event.ActionEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.lang.reflect.InvocationTargetException
import javax.swing.JOptionPane
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
import com.nomagic.magicdraw.annotation.AnnotationAction
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.validation.RuleViolationResult
import com.nomagic.magicdraw.validation.ValidationRunData
import com.nomagic.magicdraw.validation.ValidationSuiteHelper
import com.nomagic.magicdraw.validation.ui.ValidationResultPanel
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager
import com.nomagic.magicdraw.validation.ui.table.row.ValidationRuleResultTableRow
import com.nomagic.task.ProgressStatus
import com.nomagic.task.RunnableWithProgress
import com.nomagic.ui.ProgressStatusRunner
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.utils.Utilities
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import DynamicScriptsPlugin.wildCardMatch
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.AbstractAction

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

  import DynamicScriptsPlugin._

  def lookupValidationSuite( p: Project, suiteQualifiedName: String ): Option[ValidationSuiteInfo] = {
    val vsh = ValidationSuiteHelper.getInstance( p )
    vsh.getValidationSuites().find { s => wildCardMatch( s.getQualifiedName, suiteQualifiedName ) } match {
      case None                   => None
      case Some( suite: Package ) => Some( ValidationSuiteInfo( vsh, suite ) )
    }
  }

  def lookupValidationConstraint( vSuiteInfo: ValidationSuiteInfo, constraintQualifiedName: String ): Option[Constraint] =
    vSuiteInfo.vsh.getValidationRules( vSuiteInfo.suite ) find { c => wildCardMatch( c.getQualifiedName, constraintQualifiedName ) }

  def getValidationSuiteOfResult( project: Project, r: RuleViolationResult ): ValidationSuiteInfo = {
    val vsh = ValidationSuiteHelper.getInstance( project )
    val c = r.getRule
    vsh.getValidationSuites().find { p => vsh.getValidationRules( p ) contains c } match {
      case None      => throw new IllegalArgumentException( "..." )
      case Some( p ) => ValidationSuiteInfo( vsh, p )
    }
  }

  /**
   * In the MD validation result window, multiple annotation actions can be invoked from selected annotations if:
   * - all of the annotation actions have the same NMAction.getID
   * - all the annotation actions implement the MD AnnotationAction interface trait
   */
  abstract class ValidationAnnotationAction( ID: String, message: String, var annotation: Option[Annotation] = None )
    extends NMAction( ID, ID, 0 ) with AnnotationAction {

    setDescription( message )

    override def actionPerformed( e: ActionEvent ): Unit =
      annotation match {
        case Some( a ) => if ( canExecute( a ) ) execute( List( a ) ) else ()
        case None      => ()
      }

    override def canExecute( annotations: java.util.Collection[Annotation] ): Boolean =
      ( null != annotations ) && ( annotations forall ( canExecute _ ) )

    override def execute( annotations: java.util.Collection[Annotation] ): Unit =
      if ( null != annotations && !annotations.isEmpty ) {
        val sm = SessionManager.getInstance
        sm.createSession( getName )
        try {
          annotations foreach ( execute _ )
          sm.closeSession
        }
        catch {
          case _: ReadOnlyElementException =>
            sm.cancelSession
          case _: Throwable =>
            sm.cancelSession
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

  def makeAnnotationNodes4MagicDrawValidationDataResults( project: Project, r: MagicDrawValidationDataResults ): Iterable[AnnotationNodeInfo] =
    r.results map { violation =>
      AnnotationNodeInfo(
        violation.getErrorMessage,
        ValidationAnnotation(
          getValidationSuiteOfResult( project, violation ),
          violation.getRule,
          violation.getAnnotation ) )
    }

  def addActionToList( action: NMAction, actions: java.util.List[NMAction] ): java.util.List[_ <: NMAction] =
    if ( actions == null || actions.isEmpty )
      java.util.Collections.singletonList( action )
    else { actions.add( action ); actions }

  def addValidationAnnotationActionToAnnotation( action: ValidationAnnotationAction, a: Annotation ): Annotation = {
    val _a = new Annotation( a.getTargetObject, a.getConstraint, a.getText, addActionToList( action, a.getActions.asInstanceOf[java.util.List[NMAction]] ) )
    action.annotation = Some( _a )
    _a
  }

  def addValidationAnnotationActionToFirstRuleViolationResultForElement(
    action: ValidationAnnotationAction,
    e: Element,
    results: Iterable[RuleViolationResult] ): Iterable[RuleViolationResult] = {
    var added = false
    for ( result <- results )
      yield if ( e != result.getElement ) result
    else if ( added ) result
    else {
      added = true
      new RuleViolationResult( addValidationAnnotationActionToAnnotation( action, result.getAnnotation ), result.getRule )
    }
  }

  def addValidationAnnotationActionToMagicDrawValidationDataResultsException(
    action: ValidationAnnotationAction,
    element: Element,
    t: MagicDrawValidationDataResultsException ): MagicDrawValidationDataResultsException =
    MagicDrawValidationDataResultsException(
      t.validationDataResults.copy(
        results = addValidationAnnotationActionToFirstRuleViolationResultForElement( action, element, t.validationDataResults.results ) ) )

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

  /**
   * Creates a `MagicDrawValidationDataResultsException` for elements, optionally with validation annotation actions
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

  /**
   * MD's multiple annotation action invocation mechanism is limited to all annotation actions having the same ID.
   * In some cases, it would be convenient to select all annotations and invoke all of the annotation actions regardless of whether they have the same ID or not.
   * This method adds a keyboard mapping to the MD validation results window (Control + Meta + A) that will invoke all selected annotation actions (regardless of whether they have the same ID or not)
   */
  def showMDValidationDataResults( d: MagicDrawValidationDataResults ): Unit =
    if ( d.results.nonEmpty )
      Utilities.invokeAndWaitOnDispatcher( new Runnable() {
        override def run: Unit = {
          val windowTitle = d.title + System.currentTimeMillis().toString
          val runResults = new java.util.ArrayList[RuleViolationResult]( d.results )
          ValidationResultsWindowManager.updateValidationResultsWindow(
            windowTitle,
            d.title,
            d.runData,
            runResults )

          val validationWindow = Application.getInstance.getProject.getWindow( windowTitle )
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

              table.getTable.getActionMap.put( "ExecuteSelectedDynamicValidationAnnotationActions", new AbstractAction() {
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
                  
                  val messageSummary = s"Ok to execute all ${actions.size} selected dynamic validation annotion actions?\nSummary of actions by ID:\n" 
                  val messageDetails = ( actionsByID map { case (id, actions) => s" - ${actions.size} ${id} " } ) mkString ("\n")
                  
                  val status = JOptionPane.showConfirmDialog(
                    Application.getInstance.getMainFrame,
                    messageSummary + messageDetails,
                    "Dynamic Validation Annotation Actions",
                    JOptionPane.OK_CANCEL_OPTION )

                  if ( status == JOptionPane.OK_OPTION ) {
                    System.out.println( s"**** Begin executing all ${actions.size} selected annotation actions" )
                    actions foreach { action =>
                      action.actionPerformed( null )
                    }
                    System.out.println( s"**** Finished execution of all ${actions.size} selected annotation actions" )
                  }

                }
              } )

            case x =>
              System.out.println( s"*** ValidationWindow: ${x.getClass.getName}: ${x}" )
          }

        }
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
              if ( project != null && sm.isSessionCreated() )
                sm.cancelSession( project )
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
            if ( project != null && sm.isSessionCreated( project ) )
              sm.closeSession( project )
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
