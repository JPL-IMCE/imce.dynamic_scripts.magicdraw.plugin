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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes

import java.awt.Point
import java.lang.reflect.Method

import scala.language.existentials
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelShapeInstanceCreator
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.MagicDrawClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.MagicDrawElementKindDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.MagicDrawMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.MagicDrawStereotypeDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.MagicDrawStereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.ResolvedMagicDrawClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.ResolvedMagicDrawMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.ResolvedMagicDrawStereotypeDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.ResolvedMagicDrawStereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.UnresolvedMagicDrawClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.UnresolvedMagicDrawMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.UnresolvedMagicDrawStereotypeDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
trait DynamicShapeCreatorHelper {
  def isResolved: Boolean
  def createElement( project: Project ): Try[Element]
  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator ): Try[Method]
  def invokeMethod( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object
}

case class DynamicShapeCreatorForMetaclassDesignation( project: Project, d: MetaclassDesignation ) extends DynamicShapeCreatorHelper {
  val md: MagicDrawMetaclassDesignation = MagicDrawElementKindDesignation.resolveMagicDrawMetaclassDesignation( project, d )

  override def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawMetaclassDesignation => false
    case _: ResolvedMagicDrawMetaclassDesignation   => true
  }

  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   => r.createElement( project )
  }

  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname,
        classOf[Project], classOf[ToplevelShapeInstanceCreator],
        classOf[ResolvedMagicDrawClassifiedInstanceDesignation],
        classOf[PresentationElement], classOf[Point], classOf[Element] ) match {
          case m: Method => Success( m )
          case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
        }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
    }

  def invokeMethod( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   => method.invoke( null, Project.getProject( e ), das, r, pe, point, e )
  }
}

case class DynamicShapeCreatorForStereotypedMetaclassDesignation( project: Project, d: StereotypedMetaclassDesignation ) extends DynamicShapeCreatorHelper {
  val md: MagicDrawStereotypeDesignation = MagicDrawElementKindDesignation.resolveMagicDrawStereotypeDesignation( project, d )
  override def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawStereotypeDesignation => false
    case _: ResolvedMagicDrawStereotypeDesignation   => true
  }
  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawStereotypeDesignation => Failure( u.error )
    case r: ResolvedMagicDrawStereotypeDesignation   => r.createElement( project )
  }

  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname,
        classOf[Project], classOf[ToplevelShapeInstanceCreator],
        classOf[ResolvedMagicDrawClassifiedInstanceDesignation],
        classOf[PresentationElement], classOf[Point], classOf[Element] ) match {
          case m: Method => Success( m )
          case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
        }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
    }

  def invokeMethod( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   => method.invoke( null, Project.getProject( e ), das, r, pe, point, e )
  }
}

case class DynamicShapeCreatorForClassifiedInstanceDesignation( project: Project, d: ClassifiedInstanceDesignation ) extends DynamicShapeCreatorHelper {
  val md: MagicDrawClassifiedInstanceDesignation = MagicDrawElementKindDesignation.resolveMagicDrawClassifierDesignation( project, d )
  override def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawClassifiedInstanceDesignation => false
    case _: ResolvedMagicDrawClassifiedInstanceDesignation   => true
  }
  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawClassifiedInstanceDesignation => Failure( u.error )
    case r: ResolvedMagicDrawClassifiedInstanceDesignation   => r.createElement( project )
  }
  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname,
        classOf[Project], classOf[ToplevelShapeInstanceCreator],
        classOf[ResolvedMagicDrawClassifiedInstanceDesignation],
        classOf[PresentationElement], classOf[Point], classOf[Element] ) match {
          case m: Method => Success( m )
          case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
        }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
    }

  def invokeMethod( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   => method.invoke( null, Project.getProject( e ), das, r, pe, point, e )
  }
}

case class DynamicShapeCreatorForStereotypedClassifiedInstanceDesignation( project: Project, d: StereotypedClassifiedInstanceDesignation ) extends DynamicShapeCreatorHelper {
  val md: MagicDrawStereotypedClassifiedInstanceDesignation = MagicDrawElementKindDesignation.resolveMagicDrawStereotypedClassifier( project, d )
  override def isResolved: Boolean = md match {
    case _: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation => false
    case _: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation   => true
  }
  def createElement( project: Project ): Try[Element] = md match {
    case u: UnresolvedMagicDrawStereotypedClassifiedInstanceDesignation => Failure( u.error )
    case r: ResolvedMagicDrawStereotypedClassifiedInstanceDesignation   => r.createElement( project )
  }

  def lookupMethod( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname,
        classOf[Project], classOf[ToplevelShapeInstanceCreator],
        classOf[ResolvedMagicDrawStereotypedClassifiedInstanceDesignation],
        classOf[PresentationElement], classOf[Point], classOf[Element] ) match {
          case m: Method => Success( m )
          case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
        }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
    }

  def invokeMethod( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element ): Object = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   => method.invoke( null, Project.getProject( e ), das, r, pe, point, e )
  }
}
