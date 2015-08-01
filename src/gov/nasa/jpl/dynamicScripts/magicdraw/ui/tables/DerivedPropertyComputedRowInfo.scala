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

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedRowInfo(
  cs: ComputedCharacterization,
  e: Element,
  ek: MagicDrawElementKindDesignation,
  computedDerivedProperty: ComputedDerivedProperty )
  extends DerivedPropertyComputedInfo( e, ek, computedDerivedProperty ) {

  var values: Seq[AbstractTreeNodeInfo] = null

  override def dispose(): Unit = values = null

  val defaultLabel: String = computedDerivedProperty.valueType match {
    case None       => s"/${computedDerivedProperty.name.hname}"
    case Some( vt ) => s"/${computedDerivedProperty.name.hname}: ${vt.typeName.hname}"
  }

  var label: String = defaultLabel

  override def getLabel: String = label

  override def getColumnCount: Int = 1

  override def getColumnName( columnIndex: Int ): String = {
    require( columnIndex == 0 )
    getLabel
  }

  override def getRowCount: Int = if ( null == values ) 0 else values.size

  override def getValueAt( rowIndex: Int, columnIndex: Int ): Object = {
    require( columnIndex == 0 )
    require( null != values )
    require( 0 <= rowIndex && rowIndex < values.size )
    values( rowIndex )
  }

  override def update(): Seq[ValidationAnnotation] =
    if ( null != values ) {
      Seq()
    } else {
      val previousTime = System.currentTimeMillis()
      try {
        val message = computedDerivedProperty.prettyPrint( "" )+"\n"
        ClassLoaderHelper.createDynamicScriptClassLoader( computedDerivedProperty ) match {
          case Failure( t ) =>
            ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
            return Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedProperty,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedProperty],
                classOf[MagicDrawElementKindDesignation], classOf[Element] ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
                    return Seq()

                  case Success( cm ) =>
                    val result = ClassLoaderHelper.invokeAndReport(
                      previousTime, Project.getProject( e ), null, cm, ek, e )
                    result match {
                      case Failure( t ) =>
                        return Seq()
                      case Success( x ) =>
                        DerivedPropertyComputedInfo.anyToInfo( x ) match {
                          case Failure( t ) =>
                            ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
                            return Seq()
                          case Success( nodes ) =>
                            values = nodes
                            label = s"${defaultLabel} => ${values.size} values"
                            values flatMap ( _.getAnnotations )
                        }
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