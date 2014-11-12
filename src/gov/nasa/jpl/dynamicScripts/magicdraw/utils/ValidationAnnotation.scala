package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps

import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.validation.RuleViolationResult
import com.nomagic.magicdraw.validation.ValidationRunData
import com.nomagic.magicdraw.validation.ValidationSuiteHelper
import com.nomagic.task.RunnableWithProgress
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral

import gov.nasa.jpl.dynamicScripts.magicdraw.MagicDrawValidationDataResults
import gov.nasa.jpl.dynamicScripts.magicdraw.MagicDrawValidationDataResults.ValidationSuiteInfo

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class ValidationAnnotation(
  val validationSuiteInfo: ValidationSuiteInfo,
  val validationConstraint: Constraint,
  val annotation: Annotation ) {

  def toRuleViolationResult: RuleViolationResult =
    new RuleViolationResult( annotation, validationConstraint )
}

object ValidationAnnotation {

  val SEVERITY_LEVEL_ORDERING = new Ordering[EnumerationLiteral]() {

    override def compare( level1: EnumerationLiteral, level2: EnumerationLiteral ) =
      if ( level1.equals( level2 ) ) 0
      else if ( ValidationSuiteHelper.isSeverityHigherOrEqual( level1, level2 ) ) -1
      else 1

  }

  def toMagicDrawValidationDataResults(
    title: String,
    validationAnnotations: Seq[ValidationAnnotation],
    postSessionActions: java.util.Collection[RunnableWithProgress] ): MagicDrawValidationDataResults = {
    val validationSeverities = validationAnnotations map ( _.annotation.getSeverity ) toSet
    val lowestValidation = validationSeverities.toList sorted ( SEVERITY_LEVEL_ORDERING ) head
    val elements = validationAnnotations map ( _.annotation.getTarget )
    val runData = new ValidationRunData( validationAnnotations.head.validationSuiteInfo.suite, false, elements, lowestValidation )
    val ruleViolationResults = validationAnnotations map ( _.toRuleViolationResult )
    MagicDrawValidationDataResults( title, runData, ruleViolationResults, postSessionActions )
  }
}
