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

import java.lang.reflect.Method
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.paths.AssociationClassView
import com.nomagic.magicdraw.uml.symbols.paths.AssociationView
import com.nomagic.magicdraw.uml.symbols.paths.ConnectorView
import com.nomagic.magicdraw.uml.symbols.paths.DependencyView
import com.nomagic.magicdraw.uml.symbols.paths.LinkView
import com.nomagic.magicdraw.uml.symbols.paths.PathElement
import com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification
import com.nomagic.uml2.impl.ElementsFactory
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import com.nomagic.magicdraw.core.Project
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.MagicDrawElementKindDesignation

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
trait DynamicPathCreatorHelper {
  def createElement( project: Project ): Try[Element]
  def createPathElement( e: Element ): PathElement
  def lookupMethod( clazz: java.lang.Class[_], action: DynamicActionScript ): Try[Method]
  def invokeMethod( method: Method, pe: PresentationElement, e: Element ): Object
}

case class DynamicPathCreatorForMetaclassDesignation( d: MetaclassDesignation ) extends DynamicPathCreatorHelper {
  def createElement(project: Project): Try[Element] = ???
//  MagicDrawElementKindDesignation.createElement(project, d) match {
//    case Success(e) => e
//    case Failure(e) => throw e
//  }
  def createPathElement( e: Element ): PathElement = ???
  def lookupMethod(clazz: java.lang.Class[_], action: DynamicActionScript): Try[Method] = ???
  def invokeMethod(method: Method, pe: PresentationElement, e: Element): Object = ???
}

case class DynamicPathCreatorForStereotypedMetaclassDesignation( d: StereotypedMetaclassDesignation ) extends DynamicPathCreatorHelper {
  def createElement(project: Project): Try[Element] = ???
//  MagicDrawElementKindDesignation.createElement(project, d) match {
//    case Success(e) => e
//    case Failure(e) => throw e
//  }
  def createPathElement( e: Element ): PathElement = ???
  def lookupMethod(clazz: java.lang.Class[_], action: DynamicActionScript): Try[Method] = ???
  def invokeMethod(method: Method, pe: PresentationElement, e: Element): Object = ???  
}

case class DynamicPathCreatorForClassifiedInstanceDesignation( d: ClassifiedInstanceDesignation ) extends DynamicPathCreatorHelper {
  def createElement(project: Project): Try[Element] = ???
//  MagicDrawElementKindDesignation.createElement(project, d) match {
//    case Success(e) => e
//    case Failure(e) => throw e
//  }
  def createPathElement( e: Element ): PathElement = ???
  def lookupMethod(clazz: java.lang.Class[_], action: DynamicActionScript): Try[Method] = 
        try {
      clazz.getMethod( action.methodName.sname, classOf[LinkView], classOf[InstanceSpecification], classOf[Association], classOf[InstanceSpecification], classOf[InstanceSpecification] ) match {
        case m: Method => Success( m )
        case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}(LinkView, InstanceSpecification, Association, InstanceSpecification, InstanceSpecification)' not found in ${action.className.jname}" ) )
      }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}(LinkView, InstanceSpecification, Association, InstanceSpecification, InstanceSpecification)' not found in ${action.className.jname}" ) )
    }
    
  def invokeMethod(method: Method, pe: PresentationElement, e: Element): Object =
    ( pe, e ) match {
      case ( linkView: LinkView, link: InstanceSpecification ) =>
        val iSource = linkView.getClient().getElement() match {
          case i: InstanceSpecification => i
          case _                        => throw new IllegalArgumentException( s"Cannot find client InstanceSpecification" )
        }
        val iTarget = linkView.getSupplier().getElement() match {
          case i: InstanceSpecification => i
          case _                        => throw new IllegalArgumentException( s"Cannot find supplier InstanceSpecification" )
        }
        method.invoke( null, linkView, link, null, iSource, iTarget )
      case ( _, _ ) =>
        throw new IllegalArgumentException( s"Cannot find created LinkView for InstanceSpecification" )
    }
}
