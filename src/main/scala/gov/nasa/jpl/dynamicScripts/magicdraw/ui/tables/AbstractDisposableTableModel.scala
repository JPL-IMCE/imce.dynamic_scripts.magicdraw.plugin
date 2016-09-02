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

import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle
import java.lang.{Comparable, Object}
import com.jidesoft.grid.CellPainter
import com.jidesoft.grid.CellStyle
import com.jidesoft.grid.StyleModel

import com.nomagic.magicdraw.annotation.Annotation

import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper

import scala.collection.immutable._
import scala.{Boolean, Int, None, Some, Unit}
import scala.Predef.{require, String}
/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
trait AbstractDisposableTableModel 
extends Comparable[AbstractDisposableTableModel] 
with StyleModel {

  def dispose(): Unit

  def update(): Seq[ValidationAnnotation]

  def getLabel: String

  override def compareTo
  ( o: AbstractDisposableTableModel )
  : Int =
    getLabel.compareTo( o.getLabel )

  override def getCellStyleAt
  ( rowIndex: Int, columnIndex: Int )
  : CellStyle = AbstractDisposableTableModel.COMPUTED_CELL_STYLE

  override def isCellStyleOn: Boolean = true

}

object AbstractDisposableTableModel {

  val COMPUTED_CELL_STYLE = new CellStyle()

  COMPUTED_CELL_STYLE.setOverlayCellPainter( new CellPainter() {

    override def paint
    ( g: Graphics, component: Component, row: Int, column: Int, cellRect: Rectangle, value: Object )
    : Unit =
      value match {
      
      case node: AbstractTreeNodeInfo =>
        node.getAnnotations match {
          case Seq() => ()
          case validationAnnotations => 
            val validationSeverities =
              validationAnnotations.map ( _.annotation.getSeverity ).toSet
            val lowestSeverity =
              validationSeverities.toList.sorted ( MDValidationAPIHelper.SEVERITY_LEVEL_ORDERING ).head
            val lowestAnnotations =
              validationAnnotations.filter ( _.annotation.getSeverity == lowestSeverity )
            require( lowestAnnotations.nonEmpty )
            
            val annotationIcon = lowestAnnotations find ( null != _.annotation.getSeverityImageIcon ) match {
              case None =>
                ValidationAnnotation.severity2Icon(lowestSeverity)
              case Some( a ) =>
                Some( Annotation.getIcon( a.annotation ) )
            }
            
            annotationIcon match {
              case None =>
                ()
              case Some( icon ) =>
                icon.paintIcon( component, g, cellRect.x + cellRect.width - icon.getIconWidth - 1, cellRect.y )
            }
        } 
        
        case _ =>
          ()
      }
  } )

}