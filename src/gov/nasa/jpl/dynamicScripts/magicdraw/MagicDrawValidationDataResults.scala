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

import com.nomagic.magicdraw.validation.ValidationRunData
import com.nomagic.magicdraw.validation.RuleViolationResult
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.validation.ValidationSuiteHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package

import scala.language.implicitConversions._
import scala.collection.JavaConversions._
import scala.language.implicitConversions
import scala.language.postfixOps

/**
 * The MD Open API for creating validation annotations & actions requires references to
 * <<UML Standard Profile::Validation Profile::validationRule>>-stereotyped constraints defined in an
 * <<UML Standard Profile::Validation Profile::validationSuite>>-stereotyped package.
 *
 * Constructing a ValidationRunData object requires a reference to validation suite package.
 * For example, given a set of MD Annotations: Set<Annotation> annotations,
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
 * Constructing a RuleViolationResult object requires a reference to a validation constraint;
 * for example, in the context of the above:
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
 *    return new MagicDrawValidationDataResults("<title>", runData, results);
 *  }
 *
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 * @see Validation chapter in MD Open API User Manual
 */
case class MagicDrawValidationDataResults(
  title: String,
  runData: ValidationRunData,
  results: java.util.Collection[RuleViolationResult] )

object MagicDrawValidationDataResults {

  case class ValidationSuiteInfo( vsh: ValidationSuiteHelper, suite: Package ) {}
  
  def lookupValidationSuite( p: Project, suiteQualifiedName: String ): Option[ValidationSuiteInfo] = {
    val vsh = ValidationSuiteHelper.getInstance( p )
    vsh.getValidationSuites().find { s => s.getQualifiedName() == suiteQualifiedName } match {
      case None                   => None
      case Some( suite: Package ) => Some( ValidationSuiteInfo( vsh, suite ) )
    }
  }

  def lookupValidationConstraint( vSuiteInfo: ValidationSuiteInfo, constraintQualifiedName: String ): Option[Constraint] =
    vSuiteInfo.vsh.getValidationRules(vSuiteInfo.suite) find { c => c.getQualifiedName() == constraintQualifiedName }
}
