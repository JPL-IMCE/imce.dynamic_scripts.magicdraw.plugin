/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2016, California Institute of Technology ("Caltech").
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
import java.awt.Graphics
import java.awt.Rectangle
import java.lang.{Comparable, Object}
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel
import com.jidesoft.grid.CellPainter
import com.jidesoft.grid.CellStyle
import com.jidesoft.grid.StyleModel
import com.jidesoft.swing.OverlayableIconsFactory

import com.nomagic.magicdraw.annotation.Annotation

import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper

import scala.language.postfixOps
import scala.collection.immutable._
import scala.{Boolean, Int, Option, None, Some, Unit}
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