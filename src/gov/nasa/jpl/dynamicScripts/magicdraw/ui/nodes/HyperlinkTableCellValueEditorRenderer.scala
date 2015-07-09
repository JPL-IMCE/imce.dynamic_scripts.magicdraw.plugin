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
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent

import javax.swing.JOptionPane
import javax.swing.JTable

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.language.postfixOps
import scala.reflect.ClassTag

import com.jidesoft.grid.HyperlinkTableCellEditorRenderer
import com.jidesoft.grid.JideTable
import com.nomagic.magicdraw.ui.dialogs.specifications.SpecificationDialogManager
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class HyperlinkTableCellValueEditorRenderer[T]( callback: PartialFunction[( T, ActionEvent ), Unit] )( implicit tag: ClassTag[T] )
  extends HyperlinkTableCellEditorRenderer() {

  var rendererValue: Option[T] = None

  override def createTableCellEditorRendererComponent( table: JTable, row: Int, column: Int ): java.awt.Component = {
    val rComponent = super.createTableCellEditorRendererComponent( table, row, column )
    table.getValueAt( row, column ) match {
      case node: T => rendererValue = Some( node )
      case _       => rendererValue = None
    }
    rComponent
  }

  setActionListener( new ActionListener() {

    def actionPerformed( ev: ActionEvent ): Unit = rendererValue match {
      case Some( n ) => callback.lift( Tuple2( n, ev ) )
      case None      => ()
    }

  } )
}

object HyperlinkTableCellValueEditorRenderer {

  def addRenderer4AbstractTreeNodeInfo( table: JideTable ): Unit = {

    val renderer = new HyperlinkTableCellValueEditorRenderer[AbstractTreeNodeInfo](
      {
        case ( n: AnnotationNodeInfo, ev: ActionEvent ) =>
          n.getAnnotation.getTarget match {
            case e: Element =>

              if ( ( ev.getModifiers & InputEvent.SHIFT_DOWN_MASK ) == InputEvent.SHIFT_DOWN_MASK ) {
                JOptionPane.showMessageDialog( table,
                  s"${e.getHumanType}: '${e.getHumanName}' =>\n${n.getAnnotation.getText}",
                  n.getAnnotationKind,
                  n.getAnnotationMessageKind,
                  DynamicScriptsPlugin.getInstance.getJPLSmallIcon )
              }
              else {
                SpecificationDialogManager.getManager.editSpecification( e )
              }
            case _ => ()
          }

        case ( n: ReferenceNodeInfo, ev: ActionEvent ) =>
          SpecificationDialogManager.getManager.editSpecification( n.e )
      } )

    table.getColumnModel.getColumns foreach { column =>
      column.setCellEditor( renderer )
      column.setCellRenderer( renderer )
    }
  }
}