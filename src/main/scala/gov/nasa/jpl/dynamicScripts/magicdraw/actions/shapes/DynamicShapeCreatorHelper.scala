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

package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes

import java.awt.Point
import java.lang.{IllegalArgumentException, Object}
import java.lang.reflect.Method

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelShapeInstanceCreator
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.MagicDrawValidationDataResultsException

import scala.util.Failure
import scala.util.Try
import scala.{Boolean, Option}
import scala.Predef.classOf
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
trait DynamicShapeCreatorHelper {
  def isResolved
  : Boolean

  def createElement
  ( project: Project )
  : Try[Element]

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator )
  : Try[Method]

  def invokeMethod
  ( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object
}

case class DynamicShapeCreatorForMetaclassDesignation
( project: Project, d: MetaclassDesignation )
  extends DynamicShapeCreatorHelper {

  val md: MagicDrawMetaclassDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawMetaclassDesignation( project, d )

  override def isResolved
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

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator )
  : Try[Method] =
    ClassLoaderHelper.lookupMethod( clazz, action,
      classOf[Project], classOf[ToplevelShapeInstanceCreator],
      classOf[ResolvedMagicDrawMetaclassDesignation],
      classOf[PresentationElement], classOf[Point], classOf[Element] )

  def invokeMethod
  ( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => 
      Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   =>
      Option.apply(Project.getProject(e))
        .fold[Object](throw new IllegalArgumentException("Element does not belong to an MD Project!")) {
        p =>
          method.invoke(null, p, das, r, pe, point, e) match {
            case Failure(t@MagicDrawValidationDataResultsException(r)) =>
              import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
              p.showMDValidationDataResults(r)
              null
            case x =>
              x
          }
      }
  }
}

case class DynamicShapeCreatorForStereotypedMetaclassDesignation
( project: Project, d: StereotypedMetaclassDesignation )
  extends DynamicShapeCreatorHelper {

  val md: MagicDrawStereotypeDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawStereotypeDesignation( project, d )

  override def isResolved
  : Boolean = md match {
    case _: UnresolvedMagicDrawStereotypeDesignation => false
    case _: ResolvedMagicDrawStereotypeDesignation   => true
  }

  def createElement
  ( project: Project )
  : Try[Element] = md match {
    case u: UnresolvedMagicDrawStereotypeDesignation => Failure( u.error )
    case r: ResolvedMagicDrawStereotypeDesignation   => r.createElement( project )
  }

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator )
  : Try[Method] =
    ClassLoaderHelper.lookupMethod( clazz, action,
      classOf[Project], classOf[ToplevelShapeInstanceCreator],
      classOf[ResolvedMagicDrawStereotypeDesignation],
      classOf[PresentationElement], classOf[Point], classOf[Element] )

  def invokeMethod
  ( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation => 
      Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation   =>
      Option.apply(Project.getProject(e))
        .fold[Object](throw new IllegalArgumentException("Element does not belong to an MD Project!")) {
        p =>
          method.invoke(null, p, das, r, pe, point, e) match {
            case Failure(t@MagicDrawValidationDataResultsException(r)) =>
              import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
              p.showMDValidationDataResults(r)
              null
            case x =>
              x
          }
      }
  }
}

case class DynamicShapeCreatorForClassifiedInstanceDesignation
( project: Project, d: ClassifiedInstanceDesignation )
  extends DynamicShapeCreatorHelper {
  val md: MagicDrawClassifiedInstanceDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawClassifierDesignation( project, d )

  override def isResolved
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
  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator )
  : Try[Method] =
    ClassLoaderHelper.lookupMethod( clazz, action,
      classOf[Project], classOf[ToplevelShapeInstanceCreator],
      classOf[ResolvedMagicDrawClassifiedInstanceDesignation],
      classOf[PresentationElement], classOf[Point], classOf[Element] )

  def invokeMethod
  ( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object = md match {
    case u: UnresolvedMagicDrawClassifiedInstanceDesignation => 
      Failure( u.error )
    case r: ResolvedMagicDrawClassifiedInstanceDesignation   =>
      Option.apply(Project.getProject(e))
        .fold[Object](throw new IllegalArgumentException("Element does not belong to an MD Project!")) {
        p =>
          method.invoke(null, p, das, r, pe, point, e) match {
            case Failure(t@MagicDrawValidationDataResultsException(r)) =>
              import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
              p.showMDValidationDataResults(r)
              null
            case x =>
              x
          }
      }
  }
}

case class DynamicShapeCreatorForStereotypedClassifiedInstanceDesignation
( project: Project, d: StereotypedClassifiedInstanceDesignation )
  extends DynamicShapeCreatorHelper {
  val md: MagicDrawStereotypedClassifiedInstanceDesignation =
    MagicDrawElementKindDesignation.resolveMagicDrawStereotypedClassifier( project, d )

  override def isResolved
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

  def lookupMethod
  ( clazz: java.lang.Class[_], action: ToplevelShapeInstanceCreator )
  : Try[Method] =
    ClassLoaderHelper.lookupMethod( clazz, action,
      classOf[Project], classOf[ToplevelShapeInstanceCreator],
      classOf[ResolvedMagicDrawStereotypedClassifiedInstanceDesignation],
      classOf[PresentationElement], classOf[Point], classOf[Element] )

  def invokeMethod
  ( method: Method, das: ToplevelShapeInstanceCreator, pe: PresentationElement, point: Point, e: Element )
  : Object = md match {
    case u: UnresolvedMagicDrawMetaclassDesignation =>
      Failure( u.error )
    case r: ResolvedMagicDrawMetaclassDesignation =>
      Option.apply(Project.getProject(e))
        .fold[Object](throw new IllegalArgumentException("Element does not belong to an MD Project!")) {
        p =>
          method.invoke(null, p, das, r, pe, point, e) match {
            case Failure(t@MagicDrawValidationDataResultsException(r)) =>
              import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
              p.showMDValidationDataResults(r)
              null
            case x =>
              x
          }
      }
  }
}