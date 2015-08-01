/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2015, California Institute of Technology ("Caltech").
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
package gov.nasa.jpl.dynamicScripts.magicdraw.designations

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.paths._
import com.nomagic.magicdraw.uml.symbols.shapes._
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions._
import com.nomagic.uml2.ext.magicdraw.actions.mdcompleteactions._
import com.nomagic.uml2.ext.magicdraw.actions.mdintermediateactions._
import com.nomagic.uml2.ext.magicdraw.actions.mdstructuredactions._
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities._
import com.nomagic.uml2.ext.magicdraw.activities.mdcompleteactivities._
import com.nomagic.uml2.ext.magicdraw.activities.mdextrastructuredactivities._
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities._
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities._
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities._
import com.nomagic.uml2.ext.magicdraw.activities.mdstructuredactivities._
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows._
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels._
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates._
import com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses._
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies._
import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces._
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.{ Association, Class, Classifier, DataType, Diagram, Element, ElementImport, Enumeration, EnumerationLiteral, Generalization, InstanceSpecification, Operation, Package, PackageImport, PackageMerge, PrimitiveType, Slot }
import com.nomagic.uml2.ext.magicdraw.classes.mdpowertypes._
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors._
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdcommunications._
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdsimpletime._
import com.nomagic.uml2.ext.magicdraw.components.mdbasiccomponents._
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdcollaborations._
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures._
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports._
import com.nomagic.uml2.ext.magicdraw.deployments.mdartifacts._
import com.nomagic.uml2.ext.magicdraw.deployments.mdcomponentdeployments._
import com.nomagic.uml2.ext.magicdraw.deployments.mdnodes._
import com.nomagic.uml2.ext.magicdraw.interactions.mdbasicinteractions._
import com.nomagic.uml2.ext.magicdraw.interactions.mdfragments._
import com.nomagic.uml2.ext.magicdraw.mdprofiles._
import com.nomagic.uml2.ext.magicdraw.mdusecases._
import com.nomagic.uml2.ext.magicdraw.statemachines.mdbehaviorstatemachines._
import com.nomagic.uml2.ext.magicdraw.statemachines.mdprotocolstatemachines._
import com.nomagic.uml2.impl.ElementsFactory
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import scala.collection.JavaConversions._
import scala.language.existentials
import scala.language.postfixOps
import scala.util.Success
import scala.util.Failure
import scala.util.Try
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
trait MagicDrawElementKindDesignation {
  val isResolved: Boolean
  val resolutionError: Option[Throwable]
}

trait UnresolvedMagicDrawDesignation {}

trait ResolvedMagicDrawDesignation {
  def designationMatches( e: Element ): Boolean
  def createElement( project: Project ): Try[Element]
}

case class UnresolvedMagicDrawMetaclassDesignation(
  project: Project, d: MetaclassDesignation,
  error: Throwable ) extends MagicDrawMetaclassDesignation with UnresolvedMagicDrawDesignation {
  val isResolved = false
  val resolutionError = Some( error )
}

case class UnresolvedMagicDrawStereotypeDesignation(
  project: Project, d: StereotypedMetaclassDesignation,
  error: Throwable ) extends MagicDrawStereotypeDesignation with UnresolvedMagicDrawDesignation {
  val isResolved = false
  val resolutionError = Some( error )
}

case class UnresolvedMagicDrawClassifiedInstanceDesignation(
  project: Project, d: ClassifiedInstanceDesignation,
  error: Throwable ) extends MagicDrawClassifiedInstanceDesignation with UnresolvedMagicDrawDesignation {
  val isResolved = false
  val resolutionError = Some( error )
}

case class UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation(
  project: Project, d: StereotypedClassifiedInstanceDesignation,
  error: Throwable ) extends MagicDrawStereotypedClassifiedInstanceDesignation with UnresolvedMagicDrawDesignation {
  val isResolved = false
  val resolutionError = Some( error )
}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object MagicDrawElementKindDesignation {

  def getMagicDrawDesignation( p: Project, k: ElementKindDesignation ): MagicDrawElementKindDesignation = k match {
    case d: MetaclassDesignation                     => resolveMagicDrawMetaclassDesignation( p, d )
    case d: StereotypedMetaclassDesignation          => resolveMagicDrawStereotypeDesignation( p, d )
    case d: ClassifiedInstanceDesignation            => resolveMagicDrawClassifierDesignation( p, d )
    case d: StereotypedClassifiedInstanceDesignation => resolveMagicDrawStereotypedClassifier( p, d )
  }

  def resolveMagicDrawDesignation( p: Project, d: MetaclassDesignation ): MagicDrawElementKindDesignation = resolveMagicDrawMetaclassDesignation( p, d )
  def resolveMagicDrawDesignation( p: Project, d: StereotypedMetaclassDesignation ): MagicDrawElementKindDesignation = resolveMagicDrawStereotypeDesignation( p, d )
  def resolveMagicDrawDesignation( p: Project, d: ClassifiedInstanceDesignation ): MagicDrawElementKindDesignation = resolveMagicDrawClassifierDesignation( p, d )
  def resolveMagicDrawDesignation( p: Project, d: StereotypedClassifiedInstanceDesignation ): MagicDrawElementKindDesignation = resolveMagicDrawStereotypedClassifier( p, d )

  def resolveMagicDrawMetaclass( metaclassName: String ): Try[( ( ElementsFactory => Element ), java.lang.Class[_ <: Element] )] =
    ( METACLASS_NAME_2_FACTORY_METHOD get metaclassName, METACLASS_NAME_2_TYPE get metaclassName ) match {
      case ( Some( creator ), Some( metaclass ) ) => Success( ( creator, metaclass ) )
      case _                                      => Failure( new IllegalArgumentException( s"No MagicDraw metaclass named '${metaclassName}'" ) )
    }

  def resolveMagicDrawMetaclassDesignation( project: Project, d: MetaclassDesignation ): MagicDrawMetaclassDesignation =
    resolveMagicDrawMetaclass( d.metaclass.sname ) match {
      case Failure( e )        => UnresolvedMagicDrawMetaclassDesignation( project, d, e )
      case Success( ( c, m ) ) => ResolvedMagicDrawMetaclassDesignation( project, d, c, m )
    }

  def resolveMagicDrawStereotype( project: Project, metaclassName: String, profileQName: String, stereotypeQName: String ): Try[( ( ElementsFactory => Element ), java.lang.Class[_ <: Element], Profile, Stereotype )] =
    resolveMagicDrawMetaclass( metaclassName ) match {
      case Failure( e ) => Failure( e )
      case Success( ( creator, metaclass ) ) => StereotypesHelper.getProfile( project, profileQName ) match {
        case null => Failure( new IllegalArgumentException( s"No MagicDraw profile named '${profileQName}' in project '${MDUML.getProjectLocationURI( project )}'" ) )
        case pf: Profile => StereotypesHelper.getStereotype( project, stereotypeQName, pf ) match {
          case null          => Failure( new IllegalArgumentException( s"No MagicDraw stereotype named '${stereotypeQName}' in profile '${profileQName}' in project '${MDUML.getProjectLocationURI( project )}'" ) )
          case s: Stereotype => Success( ( creator, metaclass, pf, s ) )
        }
      }
    }

  def resolveMagicDrawStereotypeDesignation( project: Project, d: StereotypedMetaclassDesignation ): MagicDrawStereotypeDesignation =
    resolveMagicDrawStereotype( project, d.metaclass.sname, d.profile.qname, d.stereotype.qname ) match {
      case Failure( e )              => UnresolvedMagicDrawStereotypeDesignation( project, d, e )
      case Success( ( c, m, p, s ) ) => ResolvedMagicDrawStereotypeDesignation( project, d, c, m, p, s )
    }

  def resolveMagicDrawClassifier( project: Project, metaclassName: String, classifierQName: String ): Try[( ( ElementsFactory => Element ), java.lang.Class[_ <: Element], Classifier )] =
    resolveMagicDrawMetaclass( metaclassName ) match {
      case Failure( e ) => Failure( e )
      case Success( ( creator, metaclass ) ) => {
        val subTypes = ClassTypes.getSubtypes( classOf[Classifier], false ).toList
        for ( subType <- subTypes ) {
          ModelHelper.findElementWithPath( project, classifierQName, subType ) match {
            case cls: Classifier => 
              return Success( ( creator, metaclass, cls ) )
            case null => ()
          }
        }
        return Failure( new IllegalArgumentException( s"No MagicDraw classifier named '${classifierQName}' in project '${MDUML.getProjectLocationURI( project )}'" ) )
      }
    }

  def resolveMagicDrawClassifierDesignation( project: Project, d: ClassifiedInstanceDesignation ): MagicDrawClassifiedInstanceDesignation =
    resolveMagicDrawClassifier( project, d.metaclass.sname, d.classifier.qname ) match {
      case Failure( e )             => UnresolvedMagicDrawClassifiedInstanceDesignation( project, d, e )
      case Success( ( c, m, cls ) ) => ResolvedMagicDrawClassifiedInstanceDesignation( project, d, c, m, cls )
    }

  def resolveMagicDrawStereotypedClassifier( project: Project, metaclassName: String, classifierQName: String, profileQName: String, stereotypeQName: String ): Try[( ( ElementsFactory => Element ), java.lang.Class[_ <: Element], Classifier, Profile, Stereotype )] =
    ( resolveMagicDrawClassifier( project, metaclassName, classifierQName ), resolveMagicDrawStereotype( project, metaclassName, profileQName, stereotypeQName ) ) match {
      case ( Failure( e ), _ ) => Failure( e )
      case ( _, Failure( e ) ) => Failure( e )
      case ( Success( ( creator, metaclass, cls ) ), Success( ( _, _, pf, s ) ) ) => Success( Tuple5(creator, metaclass, cls, pf, s) )
    }

  def resolveMagicDrawStereotypedClassifier( project: Project, d: StereotypedClassifiedInstanceDesignation ): MagicDrawStereotypedClassifiedInstanceDesignation =
    resolveMagicDrawStereotypedClassifier( project, d.metaclass.sname, d.classifier.qname, d.profile.qname, d.stereotype.qname ) match {
      case Failure( e )                   => UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation( project, d, e )
      case Success( ( c, m, cls, p, s ) ) => ResolvedMagicDrawStereotypedClassifiedInstanceDesignation( project, d, c, m, cls, p, s )
    }

  /**
   * availability depends on 2 criteria:
   * - diagram type availability
   * - diagram stereotype availability
   * 
   * Diagram type availability is either:
   * - the script specifies no diagram types
   * - at least one of the script's diagram types matches the type of the current diagram
   * 
   * Diagram stereotype availability is either:
   * - the script specifies no diagram types
	 * - at least one of the script's diagram stereotypes matches a stereotype applied to the current diagram
   */
  def isDynamicContextDiagramActionScriptAvailable( 
      das: DynamicContextDiagramActionScript, 
      d: Diagram, dType: String, ds: List[Stereotype] ): Boolean = {
    val isAvailableByType = das.diagramTypes.isEmpty || ( das.diagramTypes exists ( hn => hn.hname == dType ) )
    val isAvailableByStereotype = das.diagramStereotypes.isEmpty || ( das.diagramStereotypes exists ( dsn => ds exists { s => dsn.qname == s.getQualifiedName() } ) )
    isAvailableByType && isAvailableByStereotype
  }
  
  def createClassifiedInstanceElement( project: Project, d: ElementKindDesignation, metaclassName: String, creator: ElementsFactory => Element, cls: Classifier ): Try[Element] =
    cls match {
      case a: Association =>
        metaclassName match {
          case "Connector" =>
            creator( project.getElementsFactory() ) match {
              case e: Connector =>
                e.setType( a )
                Success( e )
              case _ =>
                Failure( new IllegalArgumentException( s"${d} -- creator factory for metaclass '${metaclassName}' did not produce a Connector as expected." ) )
            }
          case "Link" | "InstanceSpecification" =>
            creator( project.getElementsFactory() ) match {
              case e: InstanceSpecification =>
                e.getClassifier().add( a )
                Success( e )
              case _ =>
                Failure( new IllegalArgumentException( s"${d} -- creator factory for metaclass '${metaclassName}' did not produce an InstanceSpecification as expected." ) )
            }
          case _ =>
            Failure( new IllegalArgumentException( s"${d} -- unsupported combination of metaclass '${metaclassName}' and classifier '${cls.getQualifiedName()}'" ) )
        }
      case c: Classifier =>
        metaclassName match {
          case "Link" | "InstanceSpecification" =>
            creator( project.getElementsFactory() ) match {
              case e: InstanceSpecification =>
                e.getClassifier().add( c )
                Success( e )
              case _ =>
                Failure( new IllegalArgumentException( s"${d} -- creator factory for metaclass '${metaclassName}' did not produce an InstanceSpecification as expected." ) )
            }
        }
      case _ =>
        Failure( new IllegalArgumentException( s"${d} -- unsupported combination of metaclass '${metaclassName}' and classifier '${cls.getQualifiedName()}'" ) )
    }

  def isConnectorClassifiedBy( e: Element, cls: Classifier ): Boolean = e match {
    case c: Connector => c.getType() == cls
    case _            => false
  }

  def isInstanceSpecificationClassifiedBy( e: Element, cls: Classifier ): Boolean = e match {
    case is: InstanceSpecification => is.getClassifier().contains( cls )
    case _                         => false
  }

  val METACLASS_2_CLASSIFIER_PREDICATE = Map[java.lang.Class[_ <: Element], ( ( Element, Classifier ) => Boolean )](
    classOf[Connector] -> isConnectorClassifiedBy,
    classOf[InstanceSpecification] -> isInstanceSpecificationClassifiedBy )

    
  def getConnectorClassifiers( e: Element ): List[Classifier] = e match {
    case c: Connector => c.getType() :: Nil
    case _            => Nil
  }

  def getInstanceSpecificationClassifiers( e: Element ): List[Classifier] = e match {
    case is: InstanceSpecification => is.getClassifier().toList
    case _                         => Nil
  }
  
  val METACLASS_2_CLASSIFIER_QUERY = Map[java.lang.Class[_ <: Element], ( ( Element ) => List[Classifier] )](
    classOf[Connector] -> getConnectorClassifiers,
    classOf[InstanceSpecification] -> getInstanceSpecificationClassifiers )

    
  val elementMC = ClassTypes.getClassType("Element")
  val allMCs = ClassTypes.getSubtypes( elementMC, true ).toSet;
  val concreteMCs = ClassTypes.getSubtypes( elementMC, false ).toSet;
  val abstractMCs = allMCs -- concreteMCs
  
  val METACLASS_2_SUPERCLASSES = concreteMCs map { cMC =>
    val superclasses = cMC :: ClassTypes.getSupertypes( cMC ).toList
    ( ClassTypes.getShortName( cMC ) -> ( superclasses map ( ClassTypes.getShortName( _ )) ) )
  } toMap;
  
  def getSuperclassesOfMetaclassShortName( metaclassShortName: String ): List[String] = {
    val superclasses = METACLASS_2_SUPERCLASSES.get( metaclassShortName )
    require( superclasses.isDefined )
    superclasses.get
  }
  
  val METACLASS_NAME_2_FACTORY_METHOD = Map[String, ElementsFactory => Element](
    "Abstraction" -> ( ( f: ElementsFactory ) => f.createAbstractionInstance() ),
    "AcceptCallAction" -> ( ( f: ElementsFactory ) => f.createAcceptCallActionInstance() ),
    "AcceptEventAction" -> ( ( f: ElementsFactory ) => f.createAcceptEventActionInstance() ),
    "ActionExecutionSpecification" -> ( ( f: ElementsFactory ) => f.createActionExecutionSpecificationInstance() ),
    "ActionInputPin" -> ( ( f: ElementsFactory ) => f.createActionInputPinInstance() ),
    "Activity" -> ( ( f: ElementsFactory ) => f.createActivityInstance() ),
    "ActivityFinalNode" -> ( ( f: ElementsFactory ) => f.createActivityFinalNodeInstance() ),
    "ActivityParameterNode" -> ( ( f: ElementsFactory ) => f.createActivityParameterNodeInstance() ),
    "ActivityPartition" -> ( ( f: ElementsFactory ) => f.createActivityPartitionInstance() ),
    "Actor" -> ( ( f: ElementsFactory ) => f.createActorInstance() ),
    "AddStructuralFeatureValueAction" -> ( ( f: ElementsFactory ) => f.createAddStructuralFeatureValueActionInstance() ),
    "AddVariableValueAction" -> ( ( f: ElementsFactory ) => f.createAddVariableValueActionInstance() ),
    "AnyReceiveEvent" -> ( ( f: ElementsFactory ) => f.createAnyReceiveEventInstance() ),
    "Artifact" -> ( ( f: ElementsFactory ) => f.createArtifactInstance() ),
    "Association" -> ( ( f: ElementsFactory ) => f.createAssociationInstance() ),
    "AssociationClass" -> ( ( f: ElementsFactory ) => f.createAssociationClassInstance() ),
    "BehaviorExecutionSpecification" -> ( ( f: ElementsFactory ) => f.createBehaviorExecutionSpecificationInstance() ),
    "BroadcastSignalAction" -> ( ( f: ElementsFactory ) => f.createBroadcastSignalActionInstance() ),
    "CallBehaviorAction" -> ( ( f: ElementsFactory ) => f.createCallBehaviorActionInstance() ),
    "CallEvent" -> ( ( f: ElementsFactory ) => f.createCallEventInstance() ),
    "CallOperationAction" -> ( ( f: ElementsFactory ) => f.createCallOperationActionInstance() ),
    "CentralBufferNode" -> ( ( f: ElementsFactory ) => f.createCentralBufferNodeInstance() ),
    "ChangeEvent" -> ( ( f: ElementsFactory ) => f.createChangeEventInstance() ),
    "Class" -> ( ( f: ElementsFactory ) => f.createClassInstance() ),
    "ClassifierTemplateParameter" -> ( ( f: ElementsFactory ) => f.createClassifierTemplateParameterInstance() ),
    "Clause" -> ( ( f: ElementsFactory ) => f.createClauseInstance() ),
    "ClearAssociationAction" -> ( ( f: ElementsFactory ) => f.createClearAssociationActionInstance() ),
    "ClearStructuralFeatureAction" -> ( ( f: ElementsFactory ) => f.createClearStructuralFeatureActionInstance() ),
    "ClearVariableAction" -> ( ( f: ElementsFactory ) => f.createClearVariableActionInstance() ),
    "Collaboration" -> ( ( f: ElementsFactory ) => f.createCollaborationInstance() ),
    "CollaborationUse" -> ( ( f: ElementsFactory ) => f.createCollaborationUseInstance() ),
    "CombinedFragment" -> ( ( f: ElementsFactory ) => f.createCombinedFragmentInstance() ),
    "Comment" -> ( ( f: ElementsFactory ) => f.createCommentInstance() ),
    "CommunicationPath" -> ( ( f: ElementsFactory ) => f.createCommunicationPathInstance() ),
    "Component" -> ( ( f: ElementsFactory ) => f.createComponentInstance() ),
    "ComponentRealization" -> ( ( f: ElementsFactory ) => f.createComponentRealizationInstance() ),
    "ConditionalNode" -> ( ( f: ElementsFactory ) => f.createConditionalNodeInstance() ),
    "ConnectableElementTemplateParameter" -> ( ( f: ElementsFactory ) => f.createConnectableElementTemplateParameterInstance() ),
    "ConnectionPointReference" -> ( ( f: ElementsFactory ) => f.createConnectionPointReferenceInstance() ),
    "Connector" -> ( ( f: ElementsFactory ) => f.createConnectorInstance() ),
    "ConnectorEnd" -> ( ( f: ElementsFactory ) => f.createConnectorEndInstance() ),
    "ConsiderIgnoreFragment" -> ( ( f: ElementsFactory ) => f.createConsiderIgnoreFragmentInstance() ),
    "Constraint" -> ( ( f: ElementsFactory ) => f.createConstraintInstance() ),
    "Continuation" -> ( ( f: ElementsFactory ) => f.createContinuationInstance() ),
    "ControlFlow" -> ( ( f: ElementsFactory ) => f.createControlFlowInstance() ),
    "CreateLinkAction" -> ( ( f: ElementsFactory ) => f.createCreateLinkActionInstance() ),
    "CreateLinkObjectAction" -> ( ( f: ElementsFactory ) => f.createCreateLinkObjectActionInstance() ),
    "CreateObjectAction" -> ( ( f: ElementsFactory ) => f.createCreateObjectActionInstance() ),
    "DataStoreNode" -> ( ( f: ElementsFactory ) => f.createDataStoreNodeInstance() ),
    "DataType" -> ( ( f: ElementsFactory ) => f.createDataTypeInstance() ),
    "DecisionNode" -> ( ( f: ElementsFactory ) => f.createDecisionNodeInstance() ),
    "Dependency" -> ( ( f: ElementsFactory ) => f.createDependencyInstance() ),
    "Deployment" -> ( ( f: ElementsFactory ) => f.createDeploymentInstance() ),
    "DeploymentSpecification" -> ( ( f: ElementsFactory ) => f.createDeploymentSpecificationInstance() ),
    "DestroyLinkAction" -> ( ( f: ElementsFactory ) => f.createDestroyLinkActionInstance() ),
    "DestroyObjectAction" -> ( ( f: ElementsFactory ) => f.createDestroyObjectActionInstance() ),
    "DestructionOccurrenceSpecification" -> ( ( f: ElementsFactory ) => f.createDestructionOccurrenceSpecificationInstance() ),
    "Device" -> ( ( f: ElementsFactory ) => f.createDeviceInstance() ),
    "Diagram" -> ( ( f: ElementsFactory ) => f.createDiagramInstance() ),
    "Duration" -> ( ( f: ElementsFactory ) => f.createDurationInstance() ),
    "DurationConstraint" -> ( ( f: ElementsFactory ) => f.createDurationConstraintInstance() ),
    "DurationInterval" -> ( ( f: ElementsFactory ) => f.createDurationIntervalInstance() ),
    "DurationObservation" -> ( ( f: ElementsFactory ) => f.createDurationObservationInstance() ),
    "ElementImport" -> ( ( f: ElementsFactory ) => f.createElementImportInstance() ),
    "ElementValue" -> ( ( f: ElementsFactory ) => f.createElementValueInstance() ),
    "Enumeration" -> ( ( f: ElementsFactory ) => f.createEnumerationInstance() ),
    "EnumerationLiteral" -> ( ( f: ElementsFactory ) => f.createEnumerationLiteralInstance() ),
    "ExceptionHandler" -> ( ( f: ElementsFactory ) => f.createExceptionHandlerInstance() ),
    "ExecutionEnvironment" -> ( ( f: ElementsFactory ) => f.createExecutionEnvironmentInstance() ),
    "ExecutionOccurrenceSpecification" -> ( ( f: ElementsFactory ) => f.createExecutionOccurrenceSpecificationInstance() ),
    "ExpansionNode" -> ( ( f: ElementsFactory ) => f.createExpansionNodeInstance() ),
    "ExpansionRegion" -> ( ( f: ElementsFactory ) => f.createExpansionRegionInstance() ),
    "Expression" -> ( ( f: ElementsFactory ) => f.createExpressionInstance() ),
    "Extend" -> ( ( f: ElementsFactory ) => f.createExtendInstance() ),
    "Extension" -> ( ( f: ElementsFactory ) => f.createExtensionInstance() ),
    "ExtensionEnd" -> ( ( f: ElementsFactory ) => f.createExtensionEndInstance() ),
    "ExtensionPoint" -> ( ( f: ElementsFactory ) => f.createExtensionPointInstance() ),
    "FinalState" -> ( ( f: ElementsFactory ) => f.createFinalStateInstance() ),
    "FlowFinalNode" -> ( ( f: ElementsFactory ) => f.createFlowFinalNodeInstance() ),
    "ForkNode" -> ( ( f: ElementsFactory ) => f.createForkNodeInstance() ),
    "FunctionBehavior" -> ( ( f: ElementsFactory ) => f.createFunctionBehaviorInstance() ),
    "Gate" -> ( ( f: ElementsFactory ) => f.createGateInstance() ),
    "GeneralOrdering" -> ( ( f: ElementsFactory ) => f.createGeneralOrderingInstance() ),
    "Generalization" -> ( ( f: ElementsFactory ) => f.createGeneralizationInstance() ),
    "GeneralizationSet" -> ( ( f: ElementsFactory ) => f.createGeneralizationSetInstance() ),
    "Image" -> ( ( f: ElementsFactory ) => f.createImageInstance() ),
    "Include" -> ( ( f: ElementsFactory ) => f.createIncludeInstance() ),
    "InformationFlow" -> ( ( f: ElementsFactory ) => f.createInformationFlowInstance() ),
    "InformationItem" -> ( ( f: ElementsFactory ) => f.createInformationItemInstance() ),
    "InitialNode" -> ( ( f: ElementsFactory ) => f.createInitialNodeInstance() ),
    "InputPin" -> ( ( f: ElementsFactory ) => f.createInputPinInstance() ),
    "InstanceSpecification" -> ( ( f: ElementsFactory ) => f.createInstanceSpecificationInstance() ),
    "InstanceValue" -> ( ( f: ElementsFactory ) => f.createInstanceValueInstance() ),
    "Interaction" -> ( ( f: ElementsFactory ) => f.createInteractionInstance() ),
    "InteractionConstraint" -> ( ( f: ElementsFactory ) => f.createInteractionConstraintInstance() ),
    "InteractionOperand" -> ( ( f: ElementsFactory ) => f.createInteractionOperandInstance() ),
    "InteractionUse" -> ( ( f: ElementsFactory ) => f.createInteractionUseInstance() ),
    "Interface" -> ( ( f: ElementsFactory ) => f.createInterfaceInstance() ),
    "InterfaceRealization" -> ( ( f: ElementsFactory ) => f.createInterfaceRealizationInstance() ),
    "InterruptibleActivityRegion" -> ( ( f: ElementsFactory ) => f.createInterruptibleActivityRegionInstance() ),
    "Interval" -> ( ( f: ElementsFactory ) => f.createIntervalInstance() ),
    "IntervalConstraint" -> ( ( f: ElementsFactory ) => f.createIntervalConstraintInstance() ),
    "JoinNode" -> ( ( f: ElementsFactory ) => f.createJoinNodeInstance() ),
    "Lifeline" -> ( ( f: ElementsFactory ) => f.createLifelineInstance() ),
    "Link" -> ( ( f: ElementsFactory ) => f.createInstanceSpecificationInstance() ),
    "LinkEndCreationData" -> ( ( f: ElementsFactory ) => f.createLinkEndCreationDataInstance() ),
    "LinkEndData" -> ( ( f: ElementsFactory ) => f.createLinkEndDataInstance() ),
    "LinkEndDestructionData" -> ( ( f: ElementsFactory ) => f.createLinkEndDestructionDataInstance() ),
    "LiteralBoolean" -> ( ( f: ElementsFactory ) => f.createLiteralBooleanInstance() ),
    "LiteralInteger" -> ( ( f: ElementsFactory ) => f.createLiteralIntegerInstance() ),
    "LiteralNull" -> ( ( f: ElementsFactory ) => f.createLiteralNullInstance() ),
    "LiteralReal" -> ( ( f: ElementsFactory ) => f.createLiteralRealInstance() ),
    "LiteralString" -> ( ( f: ElementsFactory ) => f.createLiteralStringInstance() ),
    "LiteralUnlimitedNatural" -> ( ( f: ElementsFactory ) => f.createLiteralUnlimitedNaturalInstance() ),
    "LoopNode" -> ( ( f: ElementsFactory ) => f.createLoopNodeInstance() ),
    "Manifestation" -> ( ( f: ElementsFactory ) => f.createManifestationInstance() ),
    "MergeNode" -> ( ( f: ElementsFactory ) => f.createMergeNodeInstance() ),
    "Message" -> ( ( f: ElementsFactory ) => f.createMessageInstance() ),
    "MessageOccurrenceSpecification" -> ( ( f: ElementsFactory ) => f.createMessageOccurrenceSpecificationInstance() ),
    "Model" -> ( ( f: ElementsFactory ) => f.createModelInstance() ),
    "Node" -> ( ( f: ElementsFactory ) => f.createNodeInstance() ),
    "ObjectFlow" -> ( ( f: ElementsFactory ) => f.createObjectFlowInstance() ),
    "OccurrenceSpecification" -> ( ( f: ElementsFactory ) => f.createOccurrenceSpecificationInstance() ),
    "OpaqueAction" -> ( ( f: ElementsFactory ) => f.createOpaqueActionInstance() ),
    "OpaqueBehavior" -> ( ( f: ElementsFactory ) => f.createOpaqueBehaviorInstance() ),
    "OpaqueExpression" -> ( ( f: ElementsFactory ) => f.createOpaqueExpressionInstance() ),
    "Operation" -> ( ( f: ElementsFactory ) => f.createOperationInstance() ),
    "OperationTemplateParameter" -> ( ( f: ElementsFactory ) => f.createOperationTemplateParameterInstance() ),
    "OutputPin" -> ( ( f: ElementsFactory ) => f.createOutputPinInstance() ),
    "Package" -> ( ( f: ElementsFactory ) => f.createPackageInstance() ),
    "PackageImport" -> ( ( f: ElementsFactory ) => f.createPackageImportInstance() ),
    "PackageMerge" -> ( ( f: ElementsFactory ) => f.createPackageMergeInstance() ),
    "Parameter" -> ( ( f: ElementsFactory ) => f.createParameterInstance() ),
    "ParameterSet" -> ( ( f: ElementsFactory ) => f.createParameterSetInstance() ),
    "PartDecomposition" -> ( ( f: ElementsFactory ) => f.createPartDecompositionInstance() ),
    "Port" -> ( ( f: ElementsFactory ) => f.createPortInstance() ),
    "PrimitiveType" -> ( ( f: ElementsFactory ) => f.createPrimitiveTypeInstance() ),
    "Profile" -> ( ( f: ElementsFactory ) => f.createProfileInstance() ),
    "ProfileApplication" -> ( ( f: ElementsFactory ) => f.createProfileApplicationInstance() ),
    "Property" -> ( ( f: ElementsFactory ) => f.createPropertyInstance() ),
    "ProtocolConformance" -> ( ( f: ElementsFactory ) => f.createProtocolConformanceInstance() ),
    "ProtocolStateMachine" -> ( ( f: ElementsFactory ) => f.createProtocolStateMachineInstance() ),
    "ProtocolTransition" -> ( ( f: ElementsFactory ) => f.createProtocolTransitionInstance() ),
    "Pseudostate" -> ( ( f: ElementsFactory ) => f.createPseudostateInstance() ),
    "QualifierValue" -> ( ( f: ElementsFactory ) => f.createQualifierValueInstance() ),
    "RaiseExceptionAction" -> ( ( f: ElementsFactory ) => f.createRaiseExceptionActionInstance() ),
    "ReadExtentAction" -> ( ( f: ElementsFactory ) => f.createReadExtentActionInstance() ),
    "ReadIsClassifiedObjectAction" -> ( ( f: ElementsFactory ) => f.createReadIsClassifiedObjectActionInstance() ),
    "ReadLinkAction" -> ( ( f: ElementsFactory ) => f.createReadLinkActionInstance() ),
    "ReadLinkObjectEndAction" -> ( ( f: ElementsFactory ) => f.createReadLinkObjectEndActionInstance() ),
    "ReadLinkObjectEndQualifierAction" -> ( ( f: ElementsFactory ) => f.createReadLinkObjectEndQualifierActionInstance() ),
    "ReadSelfAction" -> ( ( f: ElementsFactory ) => f.createReadSelfActionInstance() ),
    "ReadStructuralFeatureAction" -> ( ( f: ElementsFactory ) => f.createReadStructuralFeatureActionInstance() ),
    "ReadVariableAction" -> ( ( f: ElementsFactory ) => f.createReadVariableActionInstance() ),
    "Realization" -> ( ( f: ElementsFactory ) => f.createRealizationInstance() ),
    "Reception" -> ( ( f: ElementsFactory ) => f.createReceptionInstance() ),
    "ReclassifyObjectAction" -> ( ( f: ElementsFactory ) => f.createReclassifyObjectActionInstance() ),
    "RedefinableTemplateSignature" -> ( ( f: ElementsFactory ) => f.createRedefinableTemplateSignatureInstance() ),
    "ReduceAction" -> ( ( f: ElementsFactory ) => f.createReduceActionInstance() ),
    "Region" -> ( ( f: ElementsFactory ) => f.createRegionInstance() ),
    "RemoveStructuralFeatureValueAction" -> ( ( f: ElementsFactory ) => f.createRemoveStructuralFeatureValueActionInstance() ),
    "RemoveVariableValueAction" -> ( ( f: ElementsFactory ) => f.createRemoveVariableValueActionInstance() ),
    "ReplyAction" -> ( ( f: ElementsFactory ) => f.createReplyActionInstance() ),
    "SendObjectAction" -> ( ( f: ElementsFactory ) => f.createSendObjectActionInstance() ),
    "SendSignalAction" -> ( ( f: ElementsFactory ) => f.createSendSignalActionInstance() ),
    "SequenceNode" -> ( ( f: ElementsFactory ) => f.createSequenceNodeInstance() ),
    "Signal" -> ( ( f: ElementsFactory ) => f.createSignalInstance() ),
    "SignalEvent" -> ( ( f: ElementsFactory ) => f.createSignalEventInstance() ),
    "Slot" -> ( ( f: ElementsFactory ) => f.createSlotInstance() ),
    "StartClassifierBehaviorAction" -> ( ( f: ElementsFactory ) => f.createStartClassifierBehaviorActionInstance() ),
    "StartObjectBehaviorAction" -> ( ( f: ElementsFactory ) => f.createStartObjectBehaviorActionInstance() ),
    "State" -> ( ( f: ElementsFactory ) => f.createStateInstance() ),
    "StateInvariant" -> ( ( f: ElementsFactory ) => f.createStateInvariantInstance() ),
    "StateMachine" -> ( ( f: ElementsFactory ) => f.createStateMachineInstance() ),
    "Stereotype" -> ( ( f: ElementsFactory ) => f.createStereotypeInstance() ),
    "StringExpression" -> ( ( f: ElementsFactory ) => f.createStringExpressionInstance() ),
    "StructuredActivityNode" -> ( ( f: ElementsFactory ) => f.createStructuredActivityNodeInstance() ),
    "Substitution" -> ( ( f: ElementsFactory ) => f.createSubstitutionInstance() ),
    "TemplateBinding" -> ( ( f: ElementsFactory ) => f.createTemplateBindingInstance() ),
    "TemplateParameter" -> ( ( f: ElementsFactory ) => f.createTemplateParameterInstance() ),
    "TemplateParameterSubstitution" -> ( ( f: ElementsFactory ) => f.createTemplateParameterSubstitutionInstance() ),
    "TemplateSignature" -> ( ( f: ElementsFactory ) => f.createTemplateSignatureInstance() ),
    "TestIdentityAction" -> ( ( f: ElementsFactory ) => f.createTestIdentityActionInstance() ),
    "TimeConstraint" -> ( ( f: ElementsFactory ) => f.createTimeConstraintInstance() ),
    "TimeEvent" -> ( ( f: ElementsFactory ) => f.createTimeEventInstance() ),
    "TimeExpression" -> ( ( f: ElementsFactory ) => f.createTimeExpressionInstance() ),
    "TimeInterval" -> ( ( f: ElementsFactory ) => f.createTimeIntervalInstance() ),
    "TimeObservation" -> ( ( f: ElementsFactory ) => f.createTimeObservationInstance() ),
    "Transition" -> ( ( f: ElementsFactory ) => f.createTransitionInstance() ),
    "Trigger" -> ( ( f: ElementsFactory ) => f.createTriggerInstance() ),
    "UnmarshallAction" -> ( ( f: ElementsFactory ) => f.createUnmarshallActionInstance() ),
    "Usage" -> ( ( f: ElementsFactory ) => f.createUsageInstance() ),
    "UseCase" -> ( ( f: ElementsFactory ) => f.createUseCaseInstance() ),
    "ValuePin" -> ( ( f: ElementsFactory ) => f.createValuePinInstance() ),
    "ValueSpecificationAction" -> ( ( f: ElementsFactory ) => f.createValueSpecificationActionInstance() ),
    "Variable" -> ( ( f: ElementsFactory ) => f.createVariableInstance() ) )

  val METACLASS_NAME_2_SHAPE_AND_TYPE: Map[String, ( java.lang.Class[_ <: PresentationElement], java.lang.Class[_ <: Element] )] = Map(
    "Actor" -> Tuple2( classOf[ActorView], classOf[Actor] ),
    "Artifact" -> Tuple2( classOf[ArtifactView], classOf[Artifact] ),
    "CallBehaviorAction" -> Tuple2( classOf[CallBehaviorActionView], classOf[CallBehaviorAction] ),
    "CallOperationAction" -> Tuple2( classOf[CallOperationActionView], classOf[CallOperationAction] ),
    "Class" -> Tuple2( classOf[ClassView], classOf[Class] ),
    "Clause" -> Tuple2( classOf[ClauseView], classOf[Clause] ),
    "Collaboration" -> Tuple2( classOf[CollaborationView], classOf[Collaboration] ),
    "CombinedFragment" -> Tuple2( classOf[CombinedFragmentView], classOf[CombinedFragment] ),
    "Component" -> Tuple2( classOf[ComponentView], classOf[Component] ),
    "ConditionalNode" -> Tuple2( classOf[ConditionalNodeView], classOf[ConditionalNode] ),
    "ConnectorEnd" -> Tuple2( classOf[ConnectorEndView], classOf[ConnectorEnd] ),
    "DataType" -> Tuple2( classOf[DataTypeView], classOf[DataType] ),
    "DecisionNode" -> Tuple2( classOf[DecisionView], classOf[DecisionNode] ),
    "Diagram" -> Tuple2( classOf[DiagramShape], classOf[Diagram] ),
    "EnumerationLiteral" -> Tuple2( classOf[EnumerationLiteralView], classOf[EnumerationLiteral] ),
    "Enumeration" -> Tuple2( classOf[EnumerationView], classOf[Enumeration] ),
    "ExpansionNode" -> Tuple2( classOf[ExpansionNodeView], classOf[ExpansionNode] ),
    "ExpansionRegion" -> Tuple2( classOf[ExpansionRegionView], classOf[ExpansionRegion] ),
    "ExtensionPoint" -> Tuple2( classOf[ExtensionPointView], classOf[ExtensionPoint] ),
    "GeneralizationSet" -> Tuple2( classOf[GeneralizationSetView], classOf[GeneralizationSet] ),
    "InformationItem" -> Tuple2( classOf[InformationItemView], classOf[InformationItem] ),
    "InstanceSpecification" -> Tuple2( classOf[InstanceSpecificationView], classOf[InstanceSpecification] ),
    "InteractionFragment" -> Tuple2( classOf[FragmentView], classOf[InteractionFragment] ),
    "InteractionOperand" -> Tuple2( classOf[InteractionOperandView], classOf[InteractionOperand] ),
    "InteractionUse" -> Tuple2( classOf[InteractionUseView], classOf[InteractionUse] ),
    "Interface" -> Tuple2( classOf[InterfaceView], classOf[Interface] ),
    "Lifeline" -> Tuple2( classOf[LifelineView], classOf[Lifeline] ),
    "LoopNode" -> Tuple2( classOf[LoopNodeView], classOf[LoopNode] ),
    "Message" -> Tuple2( classOf[MessageView], classOf[Message] ),
    "Node" -> Tuple2( classOf[NodeView], classOf[Node] ),
    "ObjectNode" -> Tuple2( classOf[ObjectNodeView], classOf[ObjectNode] ),
    "OpaqueAction" -> Tuple2( classOf[OpaqueActionView], classOf[OpaqueAction] ),
    "Operation" -> Tuple2( classOf[OperationView], classOf[Operation] ),
    "Package" -> Tuple2( classOf[PackageView], classOf[Package] ),
    "Pin" -> Tuple2( classOf[PinView], classOf[Pin] ),
    "Port" -> Tuple2( classOf[PortView], classOf[Port] ),
    "PrimitiveType" -> Tuple2( classOf[PrimitiveTypeView], classOf[PrimitiveType] ),
    "Pseudostate" -> Tuple2( classOf[PseudoStateView], classOf[Pseudostate] ),
    "Reception" -> Tuple2( classOf[ReceptionView], classOf[Reception] ),
    "Region" -> Tuple2( classOf[RegionView], classOf[Region] ),
    "SequenceNode" -> Tuple2( classOf[SequenceNodeView], classOf[SequenceNode] ),
    "Signal" -> Tuple2( classOf[SignalView], classOf[Signal] ),
    "Slot" -> Tuple2( classOf[SlotView], classOf[Slot] ),
    "State" -> Tuple2( classOf[StateView], classOf[State] ),
    "Stereotype" -> Tuple2( classOf[StereotypeView], classOf[Stereotype] ),
    "StructuredActivityNode" -> Tuple2( classOf[StructuredActivityNodeView], classOf[StructuredActivityNode] ),
    "TemplateSignature" -> Tuple2( classOf[TemplateSignatureView], classOf[TemplateSignature] ),
    "UseCase" -> Tuple2( classOf[UseCaseView], classOf[UseCase] ) )

  val METACLASS_NAME_2_PATH_AND_TYPE: Map[String, ( java.lang.Class[_ <: PresentationElement], java.lang.Class[_ <: Element] )] = Map(

    "AssociationClass" -> Tuple2( classOf[AssociationTextBoxView], classOf[AssociationClass] ),
    "Association" -> Tuple2( classOf[AssociationView], classOf[Association] ),
    "CommunicationPath" -> Tuple2( classOf[CommunicationPathView], classOf[CommunicationPath] ),
    "Connector" -> Tuple2( classOf[ConnectorView], classOf[Connector] ),
    "ControlFlow" -> Tuple2( classOf[ControlFlowView], classOf[ControlFlow] ),
    "Dependency" -> Tuple2( classOf[DependencyView], classOf[Dependency] ),
    "Deployment" -> Tuple2( classOf[DeploymentView], classOf[Deployment] ),
    "DurationConstraint" -> Tuple2( classOf[DurationConstraintView], classOf[DurationConstraint] ),
    "ElementImport" -> Tuple2( classOf[ElementImportView], classOf[ElementImport] ),
    "ExceptionHandler" -> Tuple2( classOf[ExceptionHandlerView], classOf[ExceptionHandler] ),
    "Extend" -> Tuple2( classOf[ExtendView], classOf[Extend] ),
    "Extension" -> Tuple2( classOf[ExtensionView], classOf[Extension] ),
    "Generalization" -> Tuple2( classOf[GeneralizationView], classOf[Generalization] ),
    "Include" -> Tuple2( classOf[IncludeView], classOf[Include] ),
    "InformationFlow" -> Tuple2( classOf[InformationFlowView], classOf[InformationFlow] ),
    "InterfaceRealization" -> Tuple2( classOf[InterfaceRealizationView], classOf[InterfaceRealization] ),
    "Link" -> Tuple2( classOf[LinkView], classOf[InstanceSpecification] ),
    "ObjectFlow" -> Tuple2( classOf[ObjectFlowView], classOf[ObjectFlow] ),
    "PackageImport" -> Tuple2( classOf[PackageImportView], classOf[PackageImport] ),
    "PackageMerge" -> Tuple2( classOf[PackageMergeView], classOf[PackageMerge] ),
    "ProfileApplication" -> Tuple2( classOf[ProfileApplicationView], classOf[ProfileApplication] ),
    "Realization" -> Tuple2( classOf[RealizationView], classOf[Realization] ),
    "TemplateBinding" -> Tuple2( classOf[TemplateBindingView], classOf[TemplateBinding] ),
    "TimeConstraint" -> Tuple2( classOf[TimeConstraintView], classOf[TimeConstraint] ),
    "Transition" -> Tuple2( classOf[TransitionView], classOf[Transition] ),
    "Usage" -> Tuple2( classOf[UsageView], classOf[Usage] ) )

  val METACLASS_NAME_2_TYPE: Map[String, java.lang.Class[_ <: Element]] = {
    val m1 = METACLASS_NAME_2_SHAPE_AND_TYPE.map {
      case ( ( k, ( _, t ) ) ) => ( k -> t )
    }
    val mm1 = m1.toMap[String, java.lang.Class[_ <: Element]];
    val m2 = METACLASS_NAME_2_PATH_AND_TYPE.map {
      case ( ( k, ( _, t ) ) ) => ( k -> t )
    }
    val mm2 = m2.toMap[String, java.lang.Class[_ <: Element]];
    mm1 ++ mm2
  }
}