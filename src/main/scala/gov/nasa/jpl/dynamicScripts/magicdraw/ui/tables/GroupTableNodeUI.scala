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

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.lang.{Class, Object, StringBuffer}

import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.ScrollPaneConstants
import javax.swing.table.JTableHeader
import javax.swing.table.TableModel

import scala.collection.immutable._
import scala.collection.JavaConversions._
import scala.collection.TraversableOnce._
import scala.language.postfixOps
import scala.{Array, Boolean, Int, None, Some, StringContext, Unit}
import scala.Predef.{Map => _, Set => _, _}

import com.jidesoft.converter.ConverterContext
import com.jidesoft.converter.ObjectConverter
import com.jidesoft.converter.ObjectConverterManager
import com.jidesoft.grid.AutoResizePopupMenuCustomizer
import com.jidesoft.grid.CellPainter
import com.jidesoft.grid.CellStyle
import com.jidesoft.grid.DefaultContextSensitiveTableModel
import com.jidesoft.grid.DefaultGroupRow
import com.jidesoft.grid.DefaultGroupTableModel
import com.jidesoft.grid.FilterableTableModel
import com.jidesoft.grid.GroupTable
import com.jidesoft.grid.GroupTableHeader
import com.jidesoft.grid.GroupTablePopupMenuCustomizer
import com.jidesoft.grid.GroupTableSearchable
import com.jidesoft.grid.GroupableTableModel
import com.jidesoft.grid.JideTable
import com.jidesoft.grid.RolloverTableUtils
import com.jidesoft.grid.SelectTablePopupMenuCustomizer
import com.jidesoft.grid.StyleModel
import com.jidesoft.grid.StyledTableCellRenderer
import com.jidesoft.grid.TableColumnChooserPopupMenuCustomizer
import com.jidesoft.grid.TableHeaderPopupMenuInstaller
import com.jidesoft.grid.TableUtils
import com.jidesoft.grid.TreeTable
import com.jidesoft.grid.TreeTableUtils
import com.jidesoft.grouper.GrouperContext
import com.jidesoft.icons.IconsFactory
import com.jidesoft.swing.JideSplitPane
import com.jidesoft.swing.JideSwingUtilities
import com.jidesoft.swing.SearchableBar
import com.jidesoft.swing.StyleRange
import com.nomagic.magicdraw.annotation.Annotation

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes
import gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog.SpecificationComputedComponent
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.ReferenceNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper

/**
 * To group columns:
  *
  * @example
 * {{{
 * g = GroupTableNodeUI(...)
 * g._groupTableModel.addGroupColumn( 0, DefaultGroupTableModel.SORT_GROUP_COLUMN_ASCENDING )
 * g._groupTableModel.addGroupColumn( 1, DefaultGroupTableModel.SORT_GROUP_COLUMN_DESCENDING )
 * }}}
    * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class GroupTableNodeUI
( derived: DynamicScriptsTypes.ComputedDerivedTree,
  rows: Seq[Map[String, AbstractTreeNodeInfo]],
  columns: Seq[String] ) {

  val panel = new JideSplitPane( JideSplitPane.VERTICAL_SPLIT )

  val _tableModel = GroupTableNodeUI.treeNodeInfo2Table( derived, rows, columns )
  val filterableTableModel = new FilterableTableModel( _tableModel )

  val _groupTableModel = new DefaultGroupTableModel( filterableTableModel )
  _groupTableModel.setDisplayCountColumn( true )
  _groupTableModel.setDisplayGroupColumns( true )
  _groupTableModel.groupAndRefresh()

  val _table = new GroupTable( _groupTableModel )

  val header = new GroupTableHeader( _table )
  _table.setTableHeader( header )
  _table.setSpecialColumnsHidable( false )
  header.setGroupHeaderEnabled( true )
  header.setAutoFilterEnabled( true )
  header.setUseNativeHeaderRenderer( true )

  val installer = new TableHeaderPopupMenuInstaller( _table ) {

    override def customizeMenuItems( header: JTableHeader, popup: JPopupMenu, clickingColumn: Int ): Unit = {
      super.customizeMenuItems( header, popup, clickingColumn )
    }

  }

  installer.addTableHeaderPopupMenuCustomizer( new AutoResizePopupMenuCustomizer() )
  installer.addTableHeaderPopupMenuCustomizer( new GroupTablePopupMenuCustomizer() )
  installer.addTableHeaderPopupMenuCustomizer( new TableColumnChooserPopupMenuCustomizer() )
  installer.addTableHeaderPopupMenuCustomizer( new SelectTablePopupMenuCustomizer() )

  _table.setAutoResizeMode( JideTable.AUTO_RESIZE_FILL )

  val collapseIcon = IconsFactory.getImageIcon( classOf[GroupTable], "icons/collapse.png" )
  require( collapseIcon != null )

  val expandIcon = IconsFactory.getImageIcon( classOf[GroupTable], "icons/expand.png" )
  require( expandIcon != null )

  _table.setCollapsedIcon( collapseIcon )
  _table.setExpandedIcon( expandIcon )

  _table.setOptimized( true )

  // hide the grid lines is good for performance
  _table.setShowLeafNodeTreeLines( true )
  _table.setShowTreeLines( true )
  _table.setExpandable( true )

  _table.setDefaultCellRenderer( new StyledTableCellRenderer() {

    override def customizeStyledLabel(
      table: JTable,
      value: Object,
      isSelected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int ): Unit = {
      super.customizeStyledLabel( table, value, isSelected, hasFocus, row, column )
      val style = _table.getCellStyleAt( row, column )
      val validationAnnotations =
        for {
          c <- 0 until _groupTableModel.getColumnCount
          value = _groupTableModel.getValueAt(row, c)
        } yield value match {
          case n: AbstractTreeNodeInfo =>
            n.getAnnotations
          case _ =>
            Seq()
        }
      val validationSeverities =
        validationAnnotations.toIterable.to[Iterable].flatten.map( _.annotation.getSeverity ).to[Set]
      if ( validationSeverities.nonEmpty ) {
        val lowestSeverity =
          validationSeverities.toList.sorted( MDValidationAPIHelper.SEVERITY_LEVEL_ORDERING ).head
        ValidationAnnotation.severity2Color( lowestSeverity ) match {
          case None => ()
          case Some( color ) =>
            if ( style != null )
              addStyleRange( new StyleRange( style.getFontStyle, StyleRange.STYLE_WAVED ) )
        }
      }
    }
  } )

  val autoCellAction = new RolloverTableUtils.AutoCellAction() {

    def isRollover( table: JTable, e: MouseEvent, row: Int, column: Int ): Boolean =
      table.getValueAt( row, column ) match {
        case _: AnnotationNodeInfo => true
        case _: ReferenceNodeInfo  => true
        case _                     => false
      }

    def isEditable( table: JTable, e: MouseEvent, row: Int, column: Int ): Boolean = false

  }

  RolloverTableUtils.install( _table, autoCellAction )

  _table.expandAll()

  TableUtils.autoResizeAllColumns( _table )

  val searchable = new GroupTableSearchable( _table )
  searchable.setSearchColumnIndices( Array( 2, 3 ) )
  searchable.setRepeats( true )
  val searchableBar = SearchableBar.install(
    searchable,
    KeyStroke.getKeyStroke( KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK ),
    new SearchableBar.Installer() {
      def openSearchBar( searchableBar: SearchableBar ): Unit = {
        panel.add( searchableBar, BorderLayout.AFTER_LAST_LINE )
        panel.invalidate()
        panel.revalidate()
      }

      def closeSearchBar( searchableBar: SearchableBar ): Unit = {
        panel.remove( searchableBar )
        panel.invalidate()
        panel.revalidate()
      }
    } )
  searchableBar.setName( "TableSearchableBar" );

  val label = new JLabel( s"(${_table.getRowCount} entries) Right click on the table header to see more options" )
  panel.add( JideSwingUtilities.createLabeledComponent(
    label,
    GroupTableNodeUI.FitScrollPaneToAncestor( panel, _table, label, searchableBar ),
    BorderLayout.BEFORE_FIRST_LINE ) )

  ObjectConverterManager.initDefaultConverter()
  ObjectConverterManager.registerConverter( classOf[DefaultGroupRow], new ObjectConverter() {

    override def toString( x: Object, context: ConverterContext ): String =
      x match {
        case row: DefaultGroupRow =>
          val buf = new StringBuffer( row.toString )
          val allVisibleChildrenCount = TreeTableUtils.getDescendantCount( _table.getModel, row, true, true )
          buf.append( " (" ).append( allVisibleChildrenCount ).append( " items)" )
          buf.toString

        case _ => null
      }

    def supportToString( x: Object, context: ConverterContext ): Boolean = true

    def fromString( string: String, context: ConverterContext ): Object = null

    def supportFromString( string: String, context: ConverterContext ): Boolean = false

  } )

  val BACKGROUND1 = new Color( 159, 155, 217 )
  val BACKGROUND2 = new Color( 197, 194, 232 )
  val style1 = new CellStyle()
  style1.setBackground( BACKGROUND1 )

  val style2 = new CellStyle()
  style2.setBackground( BACKGROUND2 )

  val styleGroup1 = new CellStyle()
  val styleGroup2 = new CellStyle()

}

object GroupTableNodeUI {

  import java.awt.event.ComponentEvent
  import java.awt.event.ComponentListener

  case class FitScrollPaneToAncestor( panel: JPanel, treeTable: TreeTable, otherPanels: Component* )
    extends JScrollPane( treeTable ) with ComponentListener {

    initScrollPane()

    def initScrollPane() = {
      setBorder( BorderFactory.createLineBorder( Color.GRAY ) )
      setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED )
      setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED )
      getViewport.getView.addComponentListener( this )
    }

    override def componentMoved( e: ComponentEvent ) = ()
    override def componentShown( e: ComponentEvent ) = ()
    override def componentHidden( e: ComponentEvent ) = ()

    override def componentResized( e: ComponentEvent ) = {
      fitTreeTableToAncestorViewport( panel, treeTable, otherPanels: _* )
    }

  }

  def fitTreeTableToAncestorViewport
  ( jpanel: JPanel,
    treeTable: TreeTable,
    otherPanels: Component* )
  : Unit
  = {

    SpecificationComputedComponent.getTreeLikeHierarchicalPanelAncestorOfTable( Some( treeTable ) ) match {
      case None =>
        ()
      case Some( panel ) =>
        (
          SpecificationComputedComponent.getHierarchicalTableMainViewportAncestor( panel ),
          SpecificationComputedComponent.getHierarchicalTableContainer( treeTable ) ) match {
            case ( None, _ ) =>
              ()
            case ( _, None ) =>
              ()
            case ( Some( viewport ), Some( htable ) ) =>
              val ancestors = SpecificationComputedComponent.collectParentsUpTo( treeTable, viewport )
              val children = for {
                row <- 0 until htable.getRowCount
                child = htable.getChildComponentAt( row )
                if null != child
                if ancestors contains child
              } yield ( child, row )

              children.toList match {
                case Nil =>
                  ()
                case ( child, row ) :: _ =>
                  val tableHeaderHeight = treeTable.getTableHeader match {
                    case null    =>
                      0
                    case theader =>
                      theader.getHeight
                  }
                  val viewHeight = viewport.getHeight - tableHeaderHeight
                  val adjustedHeight = viewHeight - htable.getActualRowHeight( row )
                  val fitHeight = ( adjustedHeight /: otherPanels ) {
                    case ( hi, pi ) =>
                      hi - pi.getHeight
                  }
                  val fitRest = fitHeight % treeTable.getRowHeight()
                  val fitExact = fitHeight - fitRest
//                  val fitCheck = fitExact % treeTable.getRowHeight()
//                  require(0 == fitCheck)

                  val newSize = new Dimension()
                  val totalRowsHeight = ( 0 /: treeTable.getRowHeights.getRowHeights )( _ + _ )
                  val totalFitHeight = ( totalRowsHeight /: otherPanels ) {
                    case ( hi, pi ) => hi + pi.getHeight
                  }
                  val totalRest = totalFitHeight % treeTable.getRowHeight()
                  val totalExact = totalFitHeight - totalRest
//                  val totalCheck = totalExact % treeTable.getRowHeight()
//                  require(0 == totalCheck)

                  val newHeight = Seq( totalExact, fitExact ).min
                  newSize.setSize( viewport.getWidth, newHeight )
                  treeTable.setPreferredScrollableViewportSize( newSize )
                  panel.scrollRectToVisible( treeTable.getBounds )
                  panel.updateUI()
              }
          }
    }
  }

  def treeNodeInfo2Table
  ( derived: DynamicScriptsTypes.ComputedDerivedTree,
    rows: Seq[Map[String, AbstractTreeNodeInfo]],
    columns: Seq[String] )
  : TableModel
  = {

    require(
      derived.columnValueTypes.isDefined,
      s"A DerivedPropertyComputedTree must have explicitly-specified column value types!" )
    require(
      derived.columnValueTypes.get.nonEmpty,
      s"A DerivedPropertyComputedTree must have at least 1 column value type!" )

    val jColumns = new java.util.Vector( columns )

    val rowVectors =
      rows
      .map { row =>
        val columnInfo =
          columns
            .map { column => row.getOrElse(column, null) }
            .to[Seq]
        new java.util.Vector(columnInfo)
      }
      .to[Seq]
    val jData = new java.util.Vector( rowVectors )
    val table = new DefaultContextSensitiveTableModel( jData, jColumns ) with GroupableTableModel with StyleModel {

      val defaultLabel: String = s"/${derived.name.hname}"
      var label: String = defaultLabel

      val columnValueTypes = derived.columnValueTypes.get

      override def getColumnClass( columnIndex: Int ): Class[_] = classOf[AbstractTreeNodeInfo]

      override def isCellStyleOn: Boolean = true

      override def getCellStyleAt( rowIndex: Int, columnIndex: Int ): CellStyle = COMPUTED_CELL_STYLE

      override def getCellClassAt( row: Int, column: Int ): Class[_] = getColumnClass( column )

      override def getValueAt( row: Int, column: Int ): Object = {
        val value: java.lang.Object = super.getValueAt( row, column )
        value
      }

      override def isCellEditable( row: Int, column: Int ): Boolean = false

      override def getConverterContextAt( row: Int, column: Int ): ConverterContext =
        column match {
          case _ => super.getConverterContextAt( row, column )
        }

      override def getGrouperContext( columnIndex: Int ): GrouperContext =
        null
    }

    table
  }

  val COMPUTED_CELL_STYLE = new CellStyle()

  COMPUTED_CELL_STYLE.setOverlayCellPainter( new CellPainter() {

    override def paint
    ( g: Graphics, component: Component, row: Int, column: Int, cellRect: Rectangle, value: Object )
    : Unit
    = value match {

        case node: AbstractTreeNodeInfo =>
          node.getAnnotations match {
            case Seq() => ()
            case validationAnnotations =>
              val validationSeverities =
                validationAnnotations map ( _.annotation.getSeverity ) toSet
              val lowestSeverity =
                validationSeverities.toList.sorted( MDValidationAPIHelper.SEVERITY_LEVEL_ORDERING ).head
              val lowestAnnotations = validationAnnotations.filter ( _.annotation.getSeverity == lowestSeverity )
              require( lowestAnnotations.nonEmpty )

              val annotationIcon = lowestAnnotations find ( null != _.annotation.getSeverityImageIcon ) match {
                case None      =>
                  ValidationAnnotation.severity2Icon( lowestSeverity )
                case Some( a ) =>
                  Some( Annotation.getIcon( a.annotation ) )
              }

              annotationIcon match {
                case None         =>
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