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

package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths

import java.awt.Point
import java.lang.{IllegalArgumentException, Object}
import java.lang.reflect.Method
import scala.util.Failure
import scala.util.Try
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.paths.LinkView
import com.nomagic.magicdraw.uml.symbols.paths.PathElement
import com.nomagic.magicdraw.uml.symbols.shapes.InstanceSpecificationView
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelPathInstanceCreator
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.MagicDrawValidationDataResultsException

import scala.{AnyVal, Boolean, StringContext}
import scala.Predef.{???, classOf}

@scala.deprecated("", "")
class ElementPathFactory(val e: Element) extends AnyVal {

  def createPathElement
  : PathElement = {
    val path = new LinkView()
    path.sSetElement( e )
    path
  }

}

object ElementPathFactory {

  implicit def toELementPath(e: Element): ElementPathFactory =
  new ElementPathFactory(e)
}
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
trait DynamicPathCreatorHelper {
  def isResolved
  : Boolean

  def createElement
  ( project: Project )
  : Try[Element]

  def createPathElement
  ( e: Element )
  : PathElement

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator )
  : Try[Method]

  def invokeMethod
  ( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object
}

/**
 * @todo the path element metaclass could be, e.g., a Connector, a Dependency, ...
 * @todo define createPathElement, lookupMethod, invokeMethod
 */
case class DynamicPathCreatorForMetaclassDesignation( project: Project, d: MetaclassDesignation )
  extends DynamicPathCreatorHelper {
  val md: MagicDrawMetaclassDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawMetaclassDesignation( project, d )

  def isResolved
  : Boolean = md match {
    case _: UnresolvedMagicDrawMetaclassDesignation =>
      false
    case _: ResolvedMagicDrawMetaclassDesignation   =>
      true
  }

  def createElement
  ( project: Project )
  : Try[Element] = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation =>
      Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   =>
      r.createElement( project )
  }

  def createPathElement
  ( e: Element )
  : PathElement =
    ???

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator )
  : Try[Method] =
    ???

  def invokeMethod
  ( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object =
    ???
}

/**
 * @todo the path element metaclass could be, e.g., a Connector, a Dependency, ...
 * @todo define createPathElement, lookupMethod, invokeMethod
 */
case class DynamicPathCreatorForStereotypedMetaclassDesignation
( project:
  Project,
  d: StereotypedMetaclassDesignation )
  extends DynamicPathCreatorHelper {
  val md: MagicDrawStereotypeDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawStereotypeDesignation( project, d )

  def isResolved
  : Boolean = md match {
    case _: UnresolvedMagicDrawStereotypeDesignation =>
      false
    case _: ResolvedMagicDrawStereotypeDesignation   =>
      true
  }

  def createElement
  ( project: Project )
  : Try[Element] = md match {
    case u: UnresolvedMagicDrawStereotypeDesignation =>
      Failure( u.error )
    case r: ResolvedMagicDrawStereotypeDesignation   =>
      r.createElement( project )
  }

  def createPathElement
  ( e: Element )
  : PathElement =
    ???

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator )
  : Try[Method] =
    ???

  def invokeMethod
  ( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object =
    ???
}

/**
 * @todo check that the path element metaclass is verified to be an Association or AssociationClass
 */
case class DynamicPathCreatorForClassifiedInstanceDesignation
( project: Project,
  d: ClassifiedInstanceDesignation )
  extends DynamicPathCreatorHelper {
  val md: MagicDrawClassifiedInstanceDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawClassifierDesignation( project, d )

  def isResolved
  : Boolean = md match {
    case _: UnresolvedMagicDrawClassifiedInstanceDesignation =>
      false
    case _: ResolvedMagicDrawClassifiedInstanceDesignation   =>
      true
  }

  def createElement
  ( project: Project )
  : Try[Element] = md match {
    case u: UnresolvedMagicDrawClassifiedInstanceDesignation =>
      Failure( u.error )
    case r: ResolvedMagicDrawClassifiedInstanceDesignation   =>
      r.createElement( project )
  }

  def createPathElement
  ( e: Element )
  : PathElement = {
    import ElementPathFactory._
    e.createPathElement
  }

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator )
  : Try[Method] =
    ClassLoaderHelper.lookupMethod( clazz, action, 
        classOf[Project], classOf[ToplevelPathInstanceCreator],
        classOf[DiagramPresentationElement], classOf[Point],
        classOf[LinkView], classOf[InstanceSpecification],
        classOf[ResolvedMagicDrawClassifiedInstanceDesignation],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification] )

  def invokeMethod
  ( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object =
    md match {
      case u: UnresolvedMagicDrawClassifiedInstanceDesignation =>
        Failure( u.error )
      case r: ResolvedMagicDrawClassifiedInstanceDesignation =>
        ( pe, e ) match {
          case ( linkView: LinkView, link: InstanceSpecification ) =>
            val ( iSourceView, iSource ) = ( linkView.getClient, linkView.getClient.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) =>
                ( iv, i )
              case _ =>
                return Failure( new IllegalArgumentException( s"Cannot find client InstanceSpecification" ) )
            }
            val ( iTargetView, iTarget ) = ( linkView.getSupplier, linkView.getSupplier.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) =>
                ( iv, i )
              case _ =>
                return Failure( new IllegalArgumentException( s"Cannot find supplier InstanceSpecification" ) )
            }
            val project = Project.getProject( e )
            method.invoke( null, project, action, 
                linkView.getDiagramPresentationElement, point, 
                linkView, link, r, iSourceView, iSource, iTargetView, iTarget ) match {
              case Failure( t @ MagicDrawValidationDataResultsException( r ) ) =>
                import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
                project.showMDValidationDataResults( r )
                null
              case x => 
                x
            }
          case ( _, _ ) =>
            Failure( new IllegalArgumentException( s"Cannot find created LinkView for InstanceSpecification" ) )
        }
    }
}

/**
 * @todo check that the path element metaclass is verified to be an Association or AssociationClass
 */
case class DynamicPathCreatorForStereotypedClassifiedInstanceDesignation
( project: Project,
  d: StereotypedClassifiedInstanceDesignation )
  extends DynamicPathCreatorHelper {
  val md: MagicDrawStereotypedClassifiedInstanceDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawStereotypedClassifier( project, d )

  def isResolved
  : Boolean = md match {
    case _: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation =>
      false
    case _: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation   =>
      true
  }

  def createElement
  ( project: Project )
  : Try[Element] = md match {
    case u: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation =>
      Failure( u.error )
    case r: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation   =>
      r.createElement( project )
  }

  def createPathElement
  ( e: Element )
  : PathElement = {
    import ElementPathFactory._
    e.createPathElement
  }

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator )
  : Try[Method] =
    ClassLoaderHelper.lookupMethod( clazz, action, 
        classOf[Project], classOf[ToplevelPathInstanceCreator],
        classOf[DiagramPresentationElement], classOf[Point],
        classOf[LinkView], classOf[InstanceSpecification],
        classOf[ResolvedMagicDrawStereotypedClassifiedInstanceDesignation],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification] )
        
  def invokeMethod
  ( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object =
    md match {
      case u: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation =>
        Failure( u.error )
      case r: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation =>
        ( pe, e ) match {
          case ( linkView: LinkView, link: InstanceSpecification ) =>
            val ( iSourceView, iSource ) = ( linkView.getClient, linkView.getClient.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) =>
                ( iv, i )
              case _ =>
                return Failure( new IllegalArgumentException( s"Cannot find client InstanceSpecification" ) )
            }
            val ( iTargetView, iTarget ) = ( linkView.getSupplier, linkView.getSupplier.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) =>
                ( iv, i )
              case _ =>
                return Failure( new IllegalArgumentException( s"Cannot find supplier InstanceSpecification" ) )
            }
            val project = Project.getProject( e )
            method.invoke( null, project, action, 
                linkView.getDiagramPresentationElement, point, 
                linkView, link, r, iSourceView, iSource, iTargetView, iTarget ) match {
              case Failure( t @ MagicDrawValidationDataResultsException( r ) ) =>
                import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
                project.showMDValidationDataResults( r )
                null
              case x => 
                x
            }
          case ( _, _ ) =>
            Failure( new IllegalArgumentException( s"Cannot find created LinkView for InstanceSpecification" ) )
        }
    }
}