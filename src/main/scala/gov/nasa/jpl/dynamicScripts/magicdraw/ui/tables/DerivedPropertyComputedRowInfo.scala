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

package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.awt.event.ActionEvent
import java.lang.{Object, System, Thread}

import scala.collection.immutable._
import scala.util.{Failure, Success, Try}
import scala.{Any, Int, None, Some, StringContext, Unit}
import scala.Predef.{classOf, require, String}

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
case class DerivedPropertyComputedRowInfo
( cs: ComputedCharacterization,
  e: Element,
  ek: MagicDrawElementKindDesignation,
  computedDerivedProperty: ComputedDerivedProperty )
  extends DerivedPropertyComputedInfo( e, ek, computedDerivedProperty ) {

  private var values: Seq[AbstractTreeNodeInfo] = null

  override def dispose(): Unit = values = null

  private val defaultLabel: String = computedDerivedProperty.valueType match {
    case None       => s"/${computedDerivedProperty.name.hname}"
    case Some( vt ) => s"/${computedDerivedProperty.name.hname}: ${vt.typeName.hname}"
  }

  private var label: String = defaultLabel

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
            Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedProperty,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedProperty],
                classOf[MagicDrawElementKindDesignation], classOf[Element] ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
                    Seq()

                  case Success( cm ) =>
                    val result: Try[Any] = ClassLoaderHelper.invokeAndReport(
                      previousTime, Project.getProject( e ), null, cm, ek, e )
                    result match {
                      case Failure( t ) =>
                        Seq()
                      case Success( x ) =>
                        DerivedPropertyComputedInfo.anyToInfo( x ) match {
                          case Failure( t ) =>
                            ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
                            Seq()
                          case Success( nodes ) =>
                            values = nodes
                            label = s"$defaultLabel => ${values.size} values"
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