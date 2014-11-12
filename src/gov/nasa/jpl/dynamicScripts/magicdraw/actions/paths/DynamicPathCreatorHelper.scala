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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths

import java.awt.Point
import java.lang.reflect.Method

import scala.util.Failure
import scala.util.Success
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
import gov.nasa.jpl.dynamicScripts.magicdraw.designations.MagicDrawElementKindDesignation._

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
trait DynamicPathCreatorHelper {
  def isResolved: Boolean
  def createElement( project: Project ): Try[Element]
  def createPathElement( e: Element ): PathElement
  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator ): Try[Method]
  def invokeMethod( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object
}

/**
 * @todo the path element metaclass could be, e.g., a Connector, a Dependency, ...
 * @todo define createPathElement, lookupMethod, invokeMethod
 */
case class DynamicPathCreatorForMetaclassDesignation( project: Project, d: MetaclassDesignation ) extends DynamicPathCreatorHelper {
  val md: MagicDrawMetaclassDesignation = MagicDrawElementKindDesignation.resolveMagicDrawMetaclassDesignation( project, d )

  def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawMetaclassDesignation => false
    case _: ResolvedMagicDrawMetaclassDesignation   => true
  }

  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   => r.createElement( project )
  }

  def createPathElement( e: Element ): PathElement = ???
  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator ): Try[Method] = ???
  def invokeMethod( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object = ???
}

/**
 * @todo the path element metaclass could be, e.g., a Connector, a Dependency, ...
 * @todo define createPathElement, lookupMethod, invokeMethod
 */
case class DynamicPathCreatorForStereotypedMetaclassDesignation( project: Project, d: StereotypedMetaclassDesignation ) extends DynamicPathCreatorHelper {
  val md: MagicDrawStereotypeDesignation = MagicDrawElementKindDesignation.resolveMagicDrawStereotypeDesignation( project, d )

  def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawStereotypeDesignation => false
    case _: ResolvedMagicDrawStereotypeDesignation   => true
  }

  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawStereotypeDesignation => Failure( u.error )
    case r: ResolvedMagicDrawStereotypeDesignation   => r.createElement( project )
  }

  def createPathElement( e: Element ): PathElement = ???
  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator ): Try[Method] = ???
  def invokeMethod( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object = ???
}

/**
 * @todo check that the path element metaclass is verified to be an Association or AssociationClass
 */
case class DynamicPathCreatorForClassifiedInstanceDesignation( project: Project, d: ClassifiedInstanceDesignation ) extends DynamicPathCreatorHelper {
  val md: MagicDrawClassifiedInstanceDesignation = MagicDrawElementKindDesignation.resolveMagicDrawClassifierDesignation( project, d )

  def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawClassifiedInstanceDesignation => false
    case _: ResolvedMagicDrawClassifiedInstanceDesignation   => true
  }

  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawClassifiedInstanceDesignation => Failure( u.error )
    case r: ResolvedMagicDrawClassifiedInstanceDesignation   => r.createElement( project )
  }

  def createPathElement( e: Element ): PathElement = {
    val path = new LinkView()
    path.sSetElement( e )
    path
  }

  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname,
        classOf[Project], classOf[ToplevelPathInstanceCreator],
        classOf[DiagramPresentationElement], classOf[Point],
        classOf[LinkView], classOf[InstanceSpecification],
        classOf[ResolvedMagicDrawClassifiedInstanceDesignation],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification] ) match {
          case m: Method => Success( m )
          case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}(LinkView, InstanceSpecification, Association, InstanceSpecification, InstanceSpecification)' not found in ${action.className.jname}" ) )
        }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}(LinkView, InstanceSpecification, Association, InstanceSpecification, InstanceSpecification)' not found in ${action.className.jname}" ) )
    }

  def invokeMethod( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object =
    md match {
      case u: UnresolvedMagicDrawClassifiedInstanceDesignation => Failure( u.error )
      case r: ResolvedMagicDrawClassifiedInstanceDesignation =>
        ( pe, e ) match {
          case ( linkView: LinkView, link: InstanceSpecification ) =>
            val ( iSourceView, iSource ) = ( linkView.getClient, linkView.getClient.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) => ( iv, i )
              case _ => return Failure( new IllegalArgumentException( s"Cannot find client InstanceSpecification" ) )
            }
            val ( iTargetView, iTarget ) = ( linkView.getSupplier, linkView.getSupplier.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) => ( iv, i )
              case _ => return Failure( new IllegalArgumentException( s"Cannot find supplier InstanceSpecification" ) )
            }
            method.invoke( null, Project.getProject( e ), action, linkView.getDiagramPresentationElement, point, linkView, link, r, iSourceView, iSource, iTargetView, iTarget )
          case ( _, _ ) =>
            Failure( new IllegalArgumentException( s"Cannot find created LinkView for InstanceSpecification" ) )
        }
    }
}

/**
 * @todo check that the path element metaclass is verified to be an Association or AssociationClass
 */
case class DynamicPathCreatorForStereotypedClassifiedInstanceDesignation( project: Project, d: StereotypedClassifiedInstanceDesignation ) extends DynamicPathCreatorHelper {
  val md: MagicDrawStereotypedClassifiedInstanceDesignation = MagicDrawElementKindDesignation.resolveMagicDrawStereotypedClassifier( project, d )

  def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation => false
    case _: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation   => true
  }

  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation => Failure( u.error )
    case r: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation   => r.createElement( project )
  }

  def createPathElement( e: Element ): PathElement = {
    val path = new LinkView()
    path.sSetElement( e )
    path
  }

  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelPathInstanceCreator ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname,
        classOf[Project], classOf[ToplevelPathInstanceCreator],
        classOf[DiagramPresentationElement], classOf[Point],
        classOf[LinkView], classOf[InstanceSpecification],
        classOf[ResolvedMagicDrawStereotypedClassifiedInstanceDesignation],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification],
        classOf[InstanceSpecificationView], classOf[InstanceSpecification] ) match {
          case m: Method => Success( m )
          case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}(LinkView, InstanceSpecification, Association, InstanceSpecification, InstanceSpecification)' not found in ${action.className.jname}" ) )
        }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}(LinkView, InstanceSpecification, Association, InstanceSpecification, InstanceSpecification)' not found in ${action.className.jname}" ) )
    }

  def invokeMethod( method: Method, action: ToplevelPathInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object =
    md match {
      case u: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation => Failure( u.error )
      case r: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation =>
        ( pe, e ) match {
          case ( linkView: LinkView, link: InstanceSpecification ) =>
            val ( iSourceView, iSource ) = ( linkView.getClient, linkView.getClient.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) => ( iv, i )
              case _ => return Failure( new IllegalArgumentException( s"Cannot find client InstanceSpecification" ) )
            }
            val ( iTargetView, iTarget ) = ( linkView.getSupplier, linkView.getSupplier.getElement ) match {
              case ( iv: InstanceSpecificationView, i: InstanceSpecification ) => ( iv, i )
              case _ => return Failure( new IllegalArgumentException( s"Cannot find supplier InstanceSpecification" ) )
            }
            method.invoke( null, Project.getProject( e ), action, linkView.getDiagramPresentationElement, point, linkView, link, r, iSourceView, iSource, iTargetView, iTarget )
          case ( _, _ ) =>
            Failure( new IllegalArgumentException( s"Cannot find created LinkView for InstanceSpecification" ) )
        }
    }
}
