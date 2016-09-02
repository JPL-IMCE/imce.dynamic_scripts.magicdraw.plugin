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

package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import javax.swing.ImageIcon

import com.jidesoft.swing.OverlayableIconsFactory

import scala.collection.immutable._
import scala.collection.JavaConversions.seqAsJavaList
import scala.{Option, None, Some}
import scala.Predef.String

import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.validation.RuleViolationResult
import com.nomagic.magicdraw.validation.ValidationRunData
import com.nomagic.task.RunnableWithProgress
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.{Constraint, EnumerationLiteral, Package}

import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.MagicDrawValidationDataResults

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class ValidationAnnotation(
  val validationSuite: Package,
  val validationConstraint: Constraint,
  val annotation: Annotation ) {

  def toRuleViolationResult: RuleViolationResult =
    new RuleViolationResult( annotation, validationConstraint )
}

object ValidationAnnotation {

  def severity2Color( severity: EnumerationLiteral ): Option[String] =
    severity.getName match {
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529784621_797236_116' name='debug'/>
      case "debug"   => Some( "blue" )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529788308_980205_118' name='info'/>
      case "info"    => Some( "green" )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529789933_567569_120' name='warning'/>
      case "warning" => Some( "orange" )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529793090_656876_122' name='error'/>
      case "error"   => Some( "red" )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529795418_825036_124' name='fatal'/>
      case "fatal"   => Some( "darkred" )
      case _         => None
    }
  
  /**
   * Maps MD's SeverityKind to an OverlayableIcon
   * <packagedElement xmi:type='uml:Enumeration' xmi:id='_11_5_f720368_1159529770449_643381_97' name='SeverityKind'>
   */
  def severity2Icon( severity: EnumerationLiteral ): Option[ImageIcon] = {
    val iconFile = severity.getName match {
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529784621_797236_116' name='debug'/>
      case "debug"   => Some( OverlayableIconsFactory.CORRECT )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529788308_980205_118' name='info'/>
      case "info"    => Some( OverlayableIconsFactory.INFO )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529789933_567569_120' name='warning'/>
      case "warning" => Some( OverlayableIconsFactory.ATTENTION )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529793090_656876_122' name='error'/>
      case "error"   => Some( OverlayableIconsFactory.ERROR )
      
      // <ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='_11_5_f720368_1159529795418_825036_124' name='fatal'/>
      case "fatal"   => Some( OverlayableIconsFactory.ERROR )
      case _         => None
    }
    iconFile match {
      case None => None
      case Some( file ) => 
        OverlayableIconsFactory.getImageIcon( file ) match {
          case null => None
          case icon => Some( icon )
        }
    }
  }

  def toMagicDrawValidationDataResults(
    title: String,
    validationAnnotations: Seq[ValidationAnnotation],
    postSessionActions: java.util.Collection[RunnableWithProgress] ): Option[MagicDrawValidationDataResults] =
    if ( validationAnnotations.isEmpty ) None
    else {
      val validationSeverities =
        validationAnnotations.map ( _.annotation.getSeverity ).toSet
      val lowestValidation =
        validationSeverities.toList.sorted ( MDValidationAPIHelper.SEVERITY_LEVEL_ORDERING ).head
      val elements =
        validationAnnotations.map ( _.annotation.getTarget )
      val runData =
        new ValidationRunData( validationAnnotations.head.validationSuite, false, elements, lowestValidation )
      val ruleViolationResults =
        new java.util.ArrayList[RuleViolationResult](
          validationAnnotations
          .map( _.toRuleViolationResult ))
      Some( MagicDrawValidationDataResults( title, runData, ruleViolationResults, postSessionActions ) )
    }
}