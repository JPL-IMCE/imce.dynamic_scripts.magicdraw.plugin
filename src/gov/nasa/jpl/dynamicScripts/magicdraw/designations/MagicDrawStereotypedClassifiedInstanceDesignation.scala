/*
 * Replace this with your license text!
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
import scala.util.Success
import scala.util.Failure
import scala.util.Try

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
trait MagicDrawStereotypedClassifiedInstanceDesignation extends MagicDrawElementKindDesignation {}