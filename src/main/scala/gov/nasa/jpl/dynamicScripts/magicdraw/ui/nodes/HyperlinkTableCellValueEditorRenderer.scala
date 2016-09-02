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

package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent

import javax.swing.JOptionPane
import javax.swing.JTable

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.reflect.ClassTag
import scala.{Int, Option, None, PartialFunction, Some, StringContext, Tuple2, Unit}

import com.jidesoft.grid.HyperlinkTableCellEditorRenderer
import com.jidesoft.grid.JideTable
import com.nomagic.magicdraw.ui.dialogs.specifications.SpecificationDialogManager
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class HyperlinkTableCellValueEditorRenderer[T]
( callback: PartialFunction[( T, ActionEvent ), Unit] )
( implicit tag: ClassTag[T] )
  extends HyperlinkTableCellEditorRenderer() {

  var rendererValue: Option[T] = None

  override def createTableCellEditorRendererComponent
  ( table: JTable, row: Int, column: Int )
  : java.awt.Component = {
    val rComponent = super.createTableCellEditorRendererComponent( table, row, column )
    table.getValueAt( row, column ) match {
      case node: T => rendererValue = Some( node )
      case _       => rendererValue = None
    }
    rComponent
  }

  setActionListener( new ActionListener() {

    def actionPerformed( ev: ActionEvent ): Unit = rendererValue match {
      case Some( n ) =>
        callback.lift( Tuple2( n, ev ) )
        ()
      case None      =>
        ()
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
                  DynamicScriptsPlugin.getInstance().getJPLSmallIcon )
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