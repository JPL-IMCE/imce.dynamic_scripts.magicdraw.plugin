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
package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import javax.swing.ImageIcon

import com.jidesoft.swing.OverlayableIconsFactory

import scala.collection.immutable._
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.{Option, Ordering, None, Some}
import scala.Predef.{String}

import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.validation.RuleViolationResult
import com.nomagic.magicdraw.validation.ValidationRunData
import com.nomagic.magicdraw.validation.ValidationSuiteHelper
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