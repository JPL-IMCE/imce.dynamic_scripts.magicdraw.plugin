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
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.awt.Component
import java.awt.event.ActionEvent
import scala.language.existentials
import scala.util.Failure
import scala.util.Success
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import com.jidesoft.grid.Row
import com.jidesoft.grid.TreeTableModel
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.TreeNodeInfo
import com.nomagic.utils.Utilities

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedWidget(
  cs: ComputedCharacterization, e: Element,
  ek: MagicDrawElementKindDesignation,
  computedDerivedWidget: ComputedDerivedWidget )
  extends DerivedPropertyComputedInfo( e, ek, computedDerivedWidget ) {

  var value: Component = null

  val defaultLabel: String = s"/${computedDerivedWidget.name.hname}"
  var label: String = defaultLabel

  override def dispose(): Unit = {
    value = null
  }

  override def getLabel: String = label

  override def getColumnClass( columnIndex: Int ): Class[_] =
    classOf[Object]

  override def getColumnCount: Int = 1

  override def getColumnName( columnIndex: Int ): String =
    defaultLabel

  override def getRowCount: Int =
    if ( null == value ) 0
    else 1

  def getComponent: Component = value

  override def getValueAt( rowIndex: Int, columnIndex: Int ): Object = {
    require( columnIndex == 0 )
    require( null != value )
    require( rowIndex == 0 )
    value
  }

  override def update(): Seq[ValidationAnnotation] =
    if ( null != value ) {
      Seq()
    } else {
      val previousTime = System.currentTimeMillis()
      try {
        val message = computedDerivedWidget.prettyPrint( "" )+"\n"
        ClassLoaderHelper.createDynamicScriptClassLoader( computedDerivedWidget ) match {
          case Failure( t ) =>
            ClassLoaderHelper.reportError( computedDerivedWidget, message, t )
            return Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedWidget,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedWidget],
                classOf[MagicDrawElementKindDesignation], classOf[Element] ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedWidget, message, t )
                    return Seq()

                  case Success( cm ) =>
                    val result = ClassLoaderHelper.invokeAndReport( previousTime, Project.getProject( e ), null, cm, ek, e )
                    result match {
                      case Failure( t ) =>
                        return Seq()
                      case Success( ( component: Component, rs: Seq[_] ) ) =>
                        DerivedPropertyComputedWidget.asSeqOfValidationAnnotation( rs ) match {
                          case None =>
                            ClassLoaderHelper.reportError( computedDerivedWidget, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[ValidationAnnotation], got: ${rs.getClass.getName}" ) )
                            return Seq()
                          case Some( annotations ) =>
                            value = component
                            annotations
                        }
                      case Success( x ) =>
                        ClassLoaderHelper.reportError( computedDerivedWidget, message, new IllegalArgumentException( s"Unrecognized result -- expected: ( java.awt.Component, Seq[ValidationAnnotation] ), got: ${x.getClass.getName}" ) )
                        return Seq()
                    }
                }
            } finally {
              Thread.currentThread().setContextClassLoader( localClassLoader )
            }
          }
        }
      } finally {
        val currentTime = System.currentTimeMillis()
      }
    }

}

object DerivedPropertyComputedWidget {

  def asSeqOfValidationAnnotation( rs: Seq[_] ): Option[Seq[ValidationAnnotation]] =
    if ( rs.forall( _ match {
      case ( _: ValidationAnnotation ) => true
      case _                           => false
    } ) )
      Some( rs.asInstanceOf[Seq[ValidationAnnotation]] )
    else None
}