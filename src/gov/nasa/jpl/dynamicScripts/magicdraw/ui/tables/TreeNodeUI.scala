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

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseEvent
import java.lang.{Class, Object}

import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.ScrollPaneConstants
import javax.swing.border.EtchedBorder
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.table.JTableHeader

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.immutable._
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.{Boolean, Int, Option, None, Some, StringContext, Unit}
import scala.Predef.{classOf, intArrayOps, intWrapper, refArrayOps, require, ArrowAssoc, String}


import com.jidesoft.converter.ConverterContext
import com.jidesoft.converter.ObjectConverter
import com.jidesoft.converter.ObjectConverterManager
import com.jidesoft.grid.AutoResizePopupMenuCustomizer
import com.jidesoft.grid.CellPainter
import com.jidesoft.grid.CellStyle
import com.jidesoft.grid.DefaultExpandableRow
import com.jidesoft.grid.ExpandableRow
import com.jidesoft.grid.FilterableTreeTableModel
import com.jidesoft.grid.GroupTable
import com.jidesoft.grid.GroupTableHeader
import com.jidesoft.grid.GroupTablePopupMenuCustomizer
import com.jidesoft.grid.GroupableTableModel
import com.jidesoft.grid.ITreeTableModel
import com.jidesoft.grid.IndexChangeEvent
import com.jidesoft.grid.IndexChangeListener
import com.jidesoft.grid.QuickTableFilterField
import com.jidesoft.grid.RolloverTableUtils
import com.jidesoft.grid.SelectTablePopupMenuCustomizer
import com.jidesoft.grid.SortableTreeTableModel
import com.jidesoft.grid.StyleModel
import com.jidesoft.grid.StyledTableCellRenderer
import com.jidesoft.grid.TableColumnChooserPopupMenuCustomizer
import com.jidesoft.grid.TableHeaderPopupMenuInstaller
import com.jidesoft.grid.TableModelWrapper
import com.jidesoft.grid.TableUtils
import com.jidesoft.grid.TreeTable
import com.jidesoft.grid.TreeTableModel
import com.jidesoft.grouper.GrouperContext
import com.jidesoft.icons.IconsFactory
import com.jidesoft.swing.JideTitledBorder
import com.jidesoft.swing.PartialEtchedBorder
import com.jidesoft.swing.PartialSide
import com.jidesoft.swing.StyleRange
import com.jidesoft.swing.StyledLabel
import com.jidesoft.swing.StyledLabelBuilder
import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes
import gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog.SpecificationComputedComponent
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.LabelNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.ReferenceNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.TreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object TreeNodeUI {

  def createDerivedTreeTablePanel
  ( derived: DynamicScriptsTypes.ComputedDerivedTree, model: TreeTableModel[DerivedPropertyComputedTreeRow] )
  : ( JPanel, TreeTable ) = {

    val filterField = new QuickTableFilterField( model )
    filterField.setObjectConverterManagerEnabled( true )

    val quickSearchPanel = new JPanel( new FlowLayout( FlowLayout.LEADING ) )
    quickSearchPanel.add( filterField )
    quickSearchPanel.setBorder(
      new JideTitledBorder(
        new PartialEtchedBorder( EtchedBorder.LOWERED, PartialSide.NORTH ),
        s"Quick Filter for /${derived.name.hname} - (Right click on the table header below to see more options)",
        JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP ) )

    val tableTitleBorder = new JideTitledBorder(
      new PartialEtchedBorder( EtchedBorder.LOWERED, PartialSide.NORTH ),
      s"Filtered /${derived.name.hname}",
      JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP )
    val tablePanel = new JPanel( new BorderLayout( 2, 2 ) )
    tablePanel.setBorder( BorderFactory.createCompoundBorder(
      tableTitleBorder,
      BorderFactory.createEmptyBorder( 0, 0, 0, 0 ) ) )

    val _filterTimesLabel = new StyledLabel( "Not filtered yet." )

    val filterableTreeTableModel = new FilterableTreeTableModel( filterField.getDisplayTableModel )
    val sortableTreeTableModel = new SortableTreeTableModel( filterableTreeTableModel )

    var uniqueRows: Option[Int] = None
    var uniqueElements: Option[Int] = None
    var uniqueAnnotations: Option[Int] = None
    var uniqueTableSummary: String = ""
    def updateTableLabels(): Unit = {
      val totalExtent = uniqueRows match {
        case Some( extent ) =>
          extent
        case None =>
          val topRows = model.getRows.toSet
          val extent = topRows flatMap TreeNodeUI.getTreeRowExtent
          uniqueRows = Some( extent.size )
          val elements = extent flatMap TreeNodeUI.getDerivedPropertyComputedTreeRowElement
          uniqueElements = Some( elements.size )
          val annotations = extent flatMap TreeNodeUI.getDerivedPropertyComputedTreeRowValidationAnnotations
          uniqueAnnotations = Some( annotations.size )
          uniqueTableSummary =
            s"[Total of ${uniqueElements.get} unique elements; ${uniqueAnnotations.get} unique validation annotations]"
          extent.size
      }

      val visibleRows = filterableTreeTableModel.getRowCount
      val totalRows = filterableTreeTableModel.getActualModel.getRowCount

      if ( visibleRows == totalRows ) {
        tableTitleBorder.setTitle(
          s"Filtered /${derived.name.hname} (All $totalRows rows with $totalExtent unique values. $uniqueTableSummary" )
        StyledLabelBuilder.setStyledText(
          _filterTimesLabel,
          s"Filters in are cleared. {$totalRows:f:blue} rows shown with $totalExtent unique values." )
      }
      else {
        val visibleExtent = filterableTreeTableModel.getActualModel match {
          case tmodel: TreeTableModel[_] =>
            val expandableTreeModel = tmodel.asInstanceOf[TreeTableModel[ExpandableRow]]
            val visibleRowExtent = for {
              filteredRowIndex <- 0 until model.getRowCount
              actualRowIndex = filterableTreeTableModel.getActualRowAt( filteredRowIndex )
              row = expandableTreeModel.getRowAt( actualRowIndex )
            } yield row
            val visibleUniqueExtent = visibleRowExtent.toSet
            visibleUniqueExtent.size
          case wrapper: TableModelWrapper =>
            wrapper.getActualModel match {
              case twmodel: TreeTableModel[_] =>
                val expandableTreeWrappedModel = twmodel.asInstanceOf[TreeTableModel[ExpandableRow]]
                val visibleRowExtent = for {
                  filteredRowIndex <- 0 until model.getRowCount
                  actualRowIndex = filterableTreeTableModel.getActualRowAt( filteredRowIndex )
                  row = expandableTreeWrappedModel.getRowAt( actualRowIndex )
                } yield row
                val visibleUniqueExtent = visibleRowExtent.toSet
                visibleUniqueExtent.size
              case tw =>
                -2
            }
          case am =>
            -1
        }
        tableTitleBorder.setTitle(
          s"Filtered /${derived.name.hname} ($visibleRows visible rows with $visibleExtent unique values "+
          s"shown after applying filters to all $totalRows rows with $totalExtent unique values) $uniqueTableSummary" )
        StyledLabelBuilder.setStyledText(
          _filterTimesLabel,
          s"Filters result in {$visibleRows:f:blue} visible rows with {$visibleExtent:f:blue} "+
          s"unique values from a total of $totalRows rows with $totalExtent unique values" )
      }
    }

    sortableTreeTableModel.setDefaultSortableOption( SortableTreeTableModel.SORTABLE_LEAF_LEVEL )
    sortableTreeTableModel.setSortableOption( 0, SortableTreeTableModel.SORTABLE_ROOT_LEVEL )
    sortableTreeTableModel.addTableModelListener( new TableModelListener() {
      def tableChanged( e: TableModelEvent ): Unit =
        if ( e == null || e.getFirstRow == TableModelEvent.HEADER_ROW ) {
          updateTableLabels()
        }
    } )
    sortableTreeTableModel.addIndexChangeListener( new IndexChangeListener() {
      def indexChanged( event: IndexChangeEvent ): Unit = {
        ( event.getSource, event.getType ) match {
          case ( _: FilterableTreeTableModel[_], IndexChangeEvent.INDEX_CHANGED_EVENT ) =>
            updateTableLabels()
          case ( x, y ) =>
            ()
        }
      }
    } )

    val treeTable = new TreeTable( sortableTreeTableModel ) {
      override def scrollRectToVisible( aRect: Rectangle ) =
        SpecificationComputedComponent.scrollRectToVisible( this, aRect )

      /**
       * Intent: When the table is first displayed, do the following:
       * - expand all rows
       * - update the labels
       *
       * Why is this needed?
       *
       * Cannot find a listener that would be called when the table is first displayed
       * Tried: addTableModelListener() and addIndexChangeListener()
       */
      override def tableChanged( e: TableModelEvent ): Unit = {
        super.tableChanged( e )
        if ( e == null || e.getFirstRow == TableModelEvent.HEADER_ROW ) {
          model.expandAll()
          updateTableLabels()
        }
      }

    }

    val inputMap = treeTable.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
    inputMap.put( KeyStroke.getKeyStroke( "control Z" ), "undo" )
    inputMap.put( KeyStroke.getKeyStroke( "control shift Z" ), "redo" )

    val _header = new GroupTableHeader( treeTable )
    _header.setAutoFilterEnabled( true )
    _header.setGroupHeaderEnabled( true )
    _header.setUseNativeHeaderRenderer( true )
    treeTable.setTableHeader( _header )
    treeTable.setRowAutoResizes( true )
    treeTable.setDoubleClickEnabled( true )
    treeTable.setRowResizable( true )
    treeTable.setVariousRowHeights( true )

    filterField.setTable( treeTable )

    val infoPanel = new JPanel( new BorderLayout( 3, 3 ) )
    infoPanel.add( _filterTimesLabel )
    infoPanel.setBorder( BorderFactory.createCompoundBorder(
      new JideTitledBorder(
        new PartialEtchedBorder( EtchedBorder.LOWERED, PartialSide.NORTH ),
        "Filtered Row Count",
        JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP ),
      BorderFactory.createEmptyBorder( 0, 0, 0, 0 ) ) )

    val panel = new JPanel( new BorderLayout( 3, 3 ) )
    val scrollPane = TreeNodeUI.FitScrollPaneToAncestor( panel, treeTable, quickSearchPanel, infoPanel )
    tablePanel.add( scrollPane )

    panel.add( quickSearchPanel, BorderLayout.BEFORE_FIRST_LINE )
    panel.add( tablePanel )
    panel.add( infoPanel, BorderLayout.AFTER_LAST_LINE )

    TreeNodeUI.createTableHeaderPopupMenuInstaller( panel, treeTable, updateTableLabels(), quickSearchPanel, infoPanel )

    val collapseIcon = IconsFactory.getImageIcon( classOf[GroupTable], "icons/collapse.png" )
    require( collapseIcon != null )
    treeTable.setCollapsedIcon( collapseIcon )

    val expandIcon = IconsFactory.getImageIcon( classOf[GroupTable], "icons/expand.png" )
    require( expandIcon != null )
    treeTable.setExpandedIcon( expandIcon )

    treeTable.setOptimized( true )

    treeTable.setShowLeafNodeTreeLines( true )
    treeTable.setShowTreeLines( true )
    treeTable.setShowGrid( true )
    treeTable.setExpandable( true )

    treeTable.setDefaultCellRenderer( new StyledTableCellRenderer() {

      override def customizeStyledLabel
      ( table: JTable, value: Object, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int )
      : Unit
      = {
        super.customizeStyledLabel( table, value, isSelected, hasFocus, row, column )
        val r = model.getRowAt( row )
        val style = treeTable.getCellStyleAt( row, column )
        val validationAnnotations = r.row.values flatMap ( _.getAnnotations )
        val validationSeverities =
          validationAnnotations.map ( _.annotation.getSeverity ).toSet
        if ( validationSeverities.nonEmpty ) {
          val lowestSeverity =
            validationSeverities.toList.sorted ( MDValidationAPIHelper.SEVERITY_LEVEL_ORDERING ).head
          ValidationAnnotation.severity2Color( lowestSeverity ) match {
            case None          => ()
            case Some( color ) => addStyleRange( new StyleRange( style.getFontStyle, StyleRange.STYLE_WAVED ) )
          }
        }
      }
    } )

    // toggle expansion of tree nodes inside the treeTable
    treeTable.addTreeExpansionListener( new TreeExpansionListener() {

      def treeExpanded( event: TreeExpansionEvent ): Unit = {
        updateTableLabels()
        SpecificationComputedComponent.fitTreeTableToAncestorViewport( panel, treeTable, quickSearchPanel, infoPanel )
      }

      def treeCollapsed( event: TreeExpansionEvent ): Unit = {
        updateTableLabels()
      }
    } )

    TableUtils.autoResizeAllColumns( treeTable )

    ObjectConverterManager.initDefaultConverter()
    ObjectConverterManager.registerConverter( classOf[DerivedPropertyComputedTreeRow], new ObjectConverter() {

      override def toString( x: Object, context: ConverterContext ): String = {
        x match {
          case tree: TreeNodeInfo =>
            if ( tree.nested.isEmpty ) tree.identifier
            else s"${tree.identifier} (${tree.nested.size} children)"
          case node: AbstractTreeNodeInfo =>
            node.identifier
          case _ => ""
        }
      }

      def supportToString( x: Object, context: ConverterContext ): Boolean = true

      def fromString( string: String, context: ConverterContext ): Object = null

      def supportFromString( string: String, context: ConverterContext ): Boolean = false

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

    RolloverTableUtils.install( treeTable, autoCellAction )
    ( panel, treeTable )
  }

  def getTreeRowExtent[T <: ExpandableRow]
  ( row: T )
  ( implicit tag: ClassTag[T] )
  : Set[T]
  = row.getChildren match {
      case null => Set()
      case rows =>
        val childRows = rows flatMap {
          case childRow: T => Some(childRow)
          case _ => None
        } toSet
        val childRowExtents = childRows flatMap ( getTreeRowExtent( _ ) )
        val extent = childRows ++ childRowExtents
        extent
    }

  def getDerivedPropertyComputedTreeRowValidationAnnotations
  ( row: DerivedPropertyComputedTreeRow )
  : Set[ValidationAnnotation] = {
    val infoAnnotations = row.info.getAnnotations.toSet
    val rowAnnotations = row.row.values flatMap ( _.getAnnotations.toSet )
    Set[ValidationAnnotation]() ++ infoAnnotations ++ rowAnnotations
  }
  
  def getDerivedPropertyComputedTreeRowElement
  ( row: DerivedPropertyComputedTreeRow )
  : Set[Element] = {
    val infoElement = getAbstractTreeNodeInfoElement( row.info )
    val rowElements = row.row.values flatMap ( getAbstractTreeNodeInfoElement( _ ) )
    Set[Element]() ++ infoElement ++ rowElements
  }

  def getAbstractTreeNodeInfoElement( info: AbstractTreeNodeInfo ): Option[Element] = info match {
    case n: AnnotationNodeInfo => n.a.annotation.getTarget match {
      case e: Element => Some( e )
      case _          => None
    }
    case n: LabelNodeInfo     => None
    case n: ReferenceNodeInfo => Some( n.e )
    case n: TreeNodeInfo      => None
  }

  def createTableHeaderPopupMenuInstaller(
    panel: JPanel, treeTable: TreeTable,
    callback: => Unit,
    otherPanels: JPanel* ): TableHeaderPopupMenuInstaller = {

    val installer = new TableHeaderPopupMenuInstaller( treeTable ) {

      override def customizeMenuItems
      ( header: JTableHeader, popup: JPopupMenu, clickingColumn: Int )
      : Unit = {
        super.customizeMenuItems( header, popup, clickingColumn )

        val expandAll = new JMenuItem( new AbstractAction( "Expand All" ) {
          def actionPerformed( ev: ActionEvent ): Unit = {
            treeTable.expandAll()
            callback
          }
        } )

        val expandFirst = new JMenuItem( new AbstractAction( "Expand First Level" ) {
          def actionPerformed( ev: ActionEvent ): Unit = {
            treeTable.expandFirstLevel()
            callback
          }
        } )

        val expandNext = new JMenuItem( new AbstractAction( "Expand Next Level (or selected rows)" ) {
          def actionPerformed( ev: ActionEvent ): Unit =
            treeTable.getModel match {
              case ttm: ITreeTableModel[_] =>
                val rows = treeTable.getSelectedRows
                if ( rows.isEmpty ) {
                  treeTable.expandNextLevel()
                }
                else {
                  val treeRows = rows.toList flatMap { rowIndex =>
                    ttm.getRowAt( rowIndex ) match {
                      case treeRow: DefaultExpandableRow => Some( treeRow )
                      case _                             => None
                    }
                  }
                  treeRows.head
                    .getTreeTableModel
                    .asInstanceOf[TreeTableModel[DefaultExpandableRow]]
                    .expandRows( treeRows, true )
                }
                callback
              case _ => ()
            }
        } )

        val collapseAll = new JMenuItem( new AbstractAction( "Collapse All" ) {
          def actionPerformed( ev: ActionEvent ): Unit = {
            treeTable.collapseAll()
            callback
          }
        } )

        val collapseFirst = new JMenuItem( new AbstractAction( "Collapse First Level" ) {
          def actionPerformed( ev: ActionEvent ): Unit = {
            treeTable.collapseFirstLevel()
            callback
          }
        } )

        val collapseLast = new JMenuItem( new AbstractAction( "Collapse Last Level (or selected rows)" ) {
          def actionPerformed( ev: ActionEvent ): Unit =
            treeTable.getModel match {
              case ttm: ITreeTableModel[_] =>
                val rows = treeTable.getSelectedRows
                if ( rows.isEmpty ) {
                  treeTable.collapseLastLevel()
                }
                else {
                  val treeRows = rows.toList flatMap { rowIndex =>
                    ttm.getRowAt( rowIndex ) match {
                      case treeRow: DefaultExpandableRow => Some( treeRow )
                      case _                             => None
                    }
                  }
                  treeRows.head
                    .getTreeTableModel
                    .asInstanceOf[TreeTableModel[DefaultExpandableRow]]
                    .collapseRows( treeRows, true )
                }
                callback
              case _ => ()
            }
        } )

        val fitTable = new JMenuItem( new AbstractAction( "Fit Table" ) {
          def actionPerformed( ev: ActionEvent ): Unit = {
            //TreeNodeUI.fitTreeTableToAncestorViewport( panel, treeTable, quickSearchPanel, infoPanel )
            SpecificationComputedComponent.fitTreeTableToAncestorViewport( panel, treeTable, otherPanels: _* )
          }
        } )

        TableHeaderPopupMenuInstaller.addSeparatorIfNecessary( popup )

        popup.add( expandAll )
        popup.add( expandFirst )
        popup.add( expandNext )

        TableHeaderPopupMenuInstaller.addSeparatorIfNecessary( popup )

        popup.add( collapseAll )
        popup.add( collapseFirst )
        popup.add( collapseLast )

        TableHeaderPopupMenuInstaller.addSeparatorIfNecessary( popup )

        popup.add( fitTable )

        ()
      }
    }

    installer.addTableHeaderPopupMenuCustomizer( new AutoResizePopupMenuCustomizer() )
    installer.addTableHeaderPopupMenuCustomizer( new GroupTablePopupMenuCustomizer() )
    installer.addTableHeaderPopupMenuCustomizer( new TableColumnChooserPopupMenuCustomizer() )
    installer.addTableHeaderPopupMenuCustomizer( new SelectTablePopupMenuCustomizer() )
    installer
  }

  val cellPainter = new CellPainter() {

    override def paint
    ( g: Graphics, component: Component, row: Int, column: Int, cellRect: Rectangle, value: Object )
    : Unit
    = value match {

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
                case None      => ValidationAnnotation.severity2Icon( lowestSeverity )
                case Some( a ) => Some( Annotation.getIcon( a.annotation ) )
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
  }

  val style1 = new CellStyle()
  style1.setFontStyle( Font.BOLD )
  style1.setOverlayCellPainter( cellPainter )

  val style2 = new CellStyle()
  style2.setOverlayCellPainter( cellPainter )

  def treeNodeInfo2Table
  ( derived: DynamicScriptsTypes.ComputedDerivedTree, node: TreeNodeInfo )
  : TreeTableModel[DerivedPropertyComputedTreeRow]
  = {
    require(
      derived.columnValueTypes.isDefined,
      s"A DerivedPropertyComputedTree must have explicitly-specified column value types!" )
    require(
      derived.columnValueTypes.get.nonEmpty,
      s"A DerivedPropertyComputedTree must have at least 1 column value type!" )
    val table = new TreeTableModel[DerivedPropertyComputedTreeRow]() with GroupableTableModel with StyleModel {

      val defaultLabel: String = s"/${derived.name.hname}"
      var label: String = defaultLabel

      val columnValueTypes = derived.columnValueTypes.get

      override def getColumnClass( columnIndex: Int ): Class[_] =
        if ( 0 == columnIndex )
          classOf[DerivedPropertyComputedTreeRow]
        else {
          require( 0 < columnIndex && columnIndex <= columnValueTypes.size )
          classOf[Object]
        }

      override def getCellClassAt( row: Int, column: Int ): Class[_] = getColumnClass( column )

      override def getColumnCount: Int = 1 + columnValueTypes.size

      override def getColumnName( columnIndex: Int ): String =
        if ( 0 == columnIndex )
          defaultLabel
        else {
          require( 0 < columnIndex && columnIndex <= columnValueTypes.size )
          columnValueTypes( columnIndex - 1 ).typeName.hname
        }

      override def getValueAt( row: Int, column: Int ): Object = {
        val rowValue = super.getRowAt( row )
        //System.out.println(s"$label[$row]: $rowValue => ${(rowValue.row map { case (k,v) => s"$k=$v"}) mkString("\n - ","\n - ","\n")}")
        val value = super.getValueAt( row, column )
        //System.out.println( s"$label[$row,$column]=$value")
        value
      }

      override def isCellEditable( row: Int, column: Int ): Boolean = false

      override def getConverterContextAt( row: Int, column: Int ): ConverterContext =
        column match {
          case _ => super.getConverterContextAt( row, column )
        }

      override def isCellStyleOn: Boolean = true

      override def getCellStyleAt( row: Int, column: Int ): CellStyle = {
        val r = getRowAt( row )
        val style = if ( ( r.getParent == getRoot ) || r.children.nonEmpty ) style1 else style2
        style
      }

      override def getGrouperContext( columnIndex: Int ): GrouperContext = null
    }

    addTree( derived, table, node )

    table
  }

  def addTree
  ( derived: DynamicScriptsTypes.ComputedDerivedTree,
    model: TreeTableModel[DerivedPropertyComputedTreeRow],
    tree: TreeNodeInfo )
  : Unit
  = {

    def makeTreeRow
    ( tree: TreeNodeInfo, row: Map[String, AbstractTreeNodeInfo] )
    : DerivedPropertyComputedTreeRow
    = {
      val children = tree.nested map {
        case ( _info: AbstractTreeNodeInfo, _row: Map[String, AbstractTreeNodeInfo] ) =>
          _info match {
            case _tree: TreeNodeInfo =>
              makeTreeRow( _tree, _row )

            case _node =>
              val treeRow = DerivedPropertyComputedTreeRow( _node, _row, Seq(), derived )
              treeRow
          }
      }

      val treeRow = DerivedPropertyComputedTreeRow( tree, row, children, derived )
      treeRow
    }

    def addChildRows
    ( prefix: String, parent: DerivedPropertyComputedTreeRow )
    : Unit
    = parent.children foreach { child =>
        parent.addChild( child )
        addChildRows( prefix + " ", child )
      }

    val treeRow = makeTreeRow( tree, Map() )
    model.addRow( treeRow )
    addChildRows( " ", treeRow )

  }

  case class FitScrollPaneToAncestor( panel: JPanel, treeTable: TreeTable, otherPanels: JPanel* )
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
  ( jpanel: JPanel, treeTable: TreeTable, otherPanels: JPanel* )
  : Unit
  = {
    SpecificationComputedComponent.getTreeLikeHierarchicalPanelAncestorOfTable( Some( treeTable ) ) match {
      case None =>
        ()
      case Some( panel ) =>
        ( SpecificationComputedComponent.getHierarchicalTableMainViewportAncestor( panel ),
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
              case Nil => ()
              case ( child, row ) :: _ =>
                val rowsAbove = 0 to row
                val tableHeaderHeight = treeTable.getTableHeader match {
                  case null    => 0
                  case theader => theader.getHeight
                }
                val viewHeight = viewport.getHeight - tableHeaderHeight
                val adjustedHeight = ( viewHeight /: rowsAbove ) { case ( hi, ri ) =>
                  hi - htable.getActualRowHeight( ri )
                }
                val fitHeight = ( adjustedHeight /: otherPanels ) {
                  case ( hi, pi ) =>
                    val borderHeight = pi.getBorder match {
                      case null =>
                        0
                      case b =>
                        val bi = b.getBorderInsets( pi )
                        bi.top - bi.bottom
                    }
                    hi - pi.getHeight - borderHeight
                }
                val newSize = new Dimension()
                newSize.setSize( viewport.getWidth, fitHeight )
                treeTable.setPreferredScrollableViewportSize( newSize )
                panel.updateUI()
            }
        }
    }
  }

}