package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import javax.swing.ImageIcon
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
import com.jidesoft.swing.OverlayableIconsFactory

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
      val validationSeverities = validationAnnotations map ( _.annotation.getSeverity ) toSet
      val lowestValidation = validationSeverities.toList sorted ( SEVERITY_LEVEL_ORDERING ) head
      val elements = validationAnnotations map ( _.annotation.getTarget )
      val runData = new ValidationRunData( validationAnnotations.head.validationSuiteInfo.suite, false, elements, lowestValidation )
      val ruleViolationResults = validationAnnotations map ( _.toRuleViolationResult )
      Some( MagicDrawValidationDataResults( title, runData, ruleViolationResults, postSessionActions ) )
    }
}
