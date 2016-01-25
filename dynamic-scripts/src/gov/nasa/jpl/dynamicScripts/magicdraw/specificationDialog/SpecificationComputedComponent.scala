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
package gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.lang.{IllegalArgumentException, Integer, Object, Runnable, Thread}

import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JViewport
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities
import javax.swing.border.EtchedBorder
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableModel

import scala.Left
import scala.Right
import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.reflect.ClassTag

import scala.collection.immutable._
import scala.{Boolean, Either, Int, Option, None, Some, StringContext, Unit}
import scala.Predef.{classOf, intArrayOps, int2Integer, intWrapper, refArrayOps, require, String}

import com.jidesoft.converter.ConverterContext
import com.jidesoft.converter.ObjectConverter
import com.jidesoft.converter.ObjectConverterManager
import com.jidesoft.grid.AutoFilterTableHeader
import com.jidesoft.grid.AutoResizePopupMenuCustomizer
import com.jidesoft.grid.DefaultExpandableRow
import com.jidesoft.grid.ExpandableRow
import com.jidesoft.grid.FilterableTreeTableModel
import com.jidesoft.grid.GroupTable
import com.jidesoft.grid.GroupTableHeader
import com.jidesoft.grid.GroupTablePopupMenuCustomizer
import com.jidesoft.grid.HierarchicalTable
import com.jidesoft.grid.HierarchicalTableComponentFactory
import com.jidesoft.grid.ITreeTableModel
import com.jidesoft.grid.IndexChangeEvent
import com.jidesoft.grid.IndexChangeListener
import com.jidesoft.grid.ListSelectionModelGroup
import com.jidesoft.grid.QuickTableFilterField
import com.jidesoft.grid.RolloverTableUtils
import com.jidesoft.grid.SelectTablePopupMenuCustomizer
import com.jidesoft.grid.SortableTable
import com.jidesoft.grid.SortableTreeTableModel
import com.jidesoft.grid.StyleModel
import com.jidesoft.grid.StyledTableCellRenderer
import com.jidesoft.grid.TableColumnChooserPopupMenuCustomizer
import com.jidesoft.grid.TableHeaderPopupMenuInstaller
import com.jidesoft.grid.TableModelWrapper
import com.jidesoft.grid.TableUtils
import com.jidesoft.grid.TreeLikeHierarchicalPanel
import com.jidesoft.grid.TreeTable
import com.jidesoft.grid.TreeTableModel
import com.jidesoft.icons.IconsFactory
import com.jidesoft.swing.DefaultOverlayable
import com.jidesoft.swing.InfiniteProgressPanel
import com.jidesoft.swing.JideSwingUtilities
import com.jidesoft.swing.JideTitledBorder
import com.jidesoft.swing.OverlayableUtils
import com.jidesoft.swing.PartialEtchedBorder
import com.jidesoft.swing.PartialSide
import com.jidesoft.swing.SearchableUtils
import com.jidesoft.swing.StyleRange
import com.jidesoft.swing.StyledLabel
import com.jidesoft.swing.StyledLabelBuilder

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.ui.dialogs.specifications.ISpecificationComponent
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.validation.MagicDrawValidationDataResults
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.DoubleClickMouseListener
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.HyperlinkTableCellValueEditorRenderer
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.LabelNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.ReferenceNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.TreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.AbstractDisposableTableModel
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.AbstractHierarchicalDisposableTableModel
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.DerivedPropertyComputedTreeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.DerivedPropertyComputedTreeRow
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.DerivedPropertyComputedWidget
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.UncaughtExceptionHandler
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class SpecificationComputedComponent[T <: AbstractHierarchicalDisposableTableModel](
  model: T with AbstractTableModel, node: SpecificationComputedNode[T] )
  extends ISpecificationComponent {

  override def propertyChanged( element: Element, event: PropertyChangeEvent ) = ()
  override def updateComponent = ()
  override def dispose = model.dispose

  val _group = new ListSelectionModelGroup()

  override def getComponent: JComponent = {
    val table = new HierarchicalTable( model )
    table.setExpandIconVisible( true )
    table.setAutoRefreshOnRowUpdate( true )
    table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION )
    table.setRowResizable( true )
    table.setVariousRowHeights( true )
    //table.setAutoResizeMode( JideTable.AUTO_RESIZE_FILL )
    table.setComponentFactory( HierarchicaltableComputedComponentFactory() )
    _group.add( table.getSelectionModel )

    val header = new AutoFilterTableHeader( table )
    table.setTableHeader( header )
    header.setAutoFilterEnabled( true )
    header.setUseNativeHeaderRenderer( true )

    val scrollPane = new JScrollPane( table )
    scrollPane.getViewport.putClientProperty( "HierarchicalTable.mainViewport", Boolean.box( true ) )
    scrollPane
  }

  case class StyleTableModel() extends TableModel with StyleModel {

    override def getRowCount = 0
    override def getColumnCount = model.getColumnCount
    override def getColumnName( columnIndex: Int ) = model.getColumnName( columnIndex )
    override def getColumnClass( columnIndex: Int ) = model.getColumnClass( columnIndex )
    override def getCellStyleAt( row: Int, column: Int ) = model.getCellStyleAt( row, column )
    override def isCellStyleOn = true
    override def isCellEditable( rowIndex: Int, columnIndex: Int ) = false
    override def getValueAt( rowIndex: Int, columnIndex: Int ) = null
    override def setValueAt( value: Object, rowIndex: Int, columnIndex: Int ) = ()
    override def addTableModelListener( l: TableModelListener ) = ()
    override def removeTableModelListener( l: TableModelListener ) = ()
  }

  case class HierarchicaltableComputedComponentFactory() extends HierarchicalTableComponentFactory {

    val _tableModels = scala.collection.mutable.HashMap[Integer, TableModel]()
    val _tables = scala.collection.mutable.HashMap[Integer, JTable]()

    override def createChildComponent( _table: HierarchicalTable, value: Object, row: Int ): Component =
      value match {
        case widget: DerivedPropertyComputedWidget =>
          createChildWidgetComponent( _table, widget, row )
        case tree: DerivedPropertyComputedTreeInfo =>
          createChildTreeComponent( _table, tree, row )
        case model: AbstractDisposableTableModel with AbstractTableModel =>
          createChildTableComponent( _table, model, row )
        case x =>
          throw new IllegalArgumentException( s" createChildComponent: unrecognized value: $x" )
      }

    def createChildWidgetComponent
    ( _htable: HierarchicalTable, model: DerivedPropertyComputedWidget, row: Int )
    : Component = {

      val widgetPanel = new TreeLikeHierarchicalPanel()
      val overlayable = new DefaultOverlayable( widgetPanel )

      val progressPanel = new InfiniteProgressPanel() {
        override def getPreferredSize = new Dimension( 20, 20 )
      }

      overlayable.addOverlayComponent( progressPanel )
      progressPanel.start()
      overlayable.setOverlayVisible( true )

      val thread = new Thread() {
        override def run: Unit = {
          super.run()

          UncaughtExceptionHandler( model.getLabel )
          val annotations = model.update()

          SwingUtilities.invokeLater( new Runnable() {
            override def run: Unit = {
              model.fireTableDataChanged()

              model.getComponent match {
                case null =>
                  ()
                case c =>
                  widgetPanel.add( c )

                  overlayable.setOverlayVisible( false )
                  overlayable.getOverlayComponents foreach {
                    case comp: InfiniteProgressPanel =>
                      comp.stop()
                    case _ =>
                      ()
                  }

                  _htable.doLayout()

                  val validationDataResults = ValidationAnnotation.toMagicDrawValidationDataResults(
                    title = model.getLabel,
                    validationAnnotations = annotations,
                    postSessionActions = Nil )
                  import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
                  model.getProject.showMDValidationDataResultsIfAny( validationDataResults )
              }
            }
          } )

        }
      }
      thread.start()
      overlayable
    }

    def createChildTreeComponent
    ( _htable: HierarchicalTable, model: DerivedPropertyComputedTreeInfo, row: Int )
    : Component = {

      val treePanel = new TreeLikeHierarchicalPanel()
      val overlayable = new DefaultOverlayable( treePanel )

      val progressPanel = new InfiniteProgressPanel() {
        override def getPreferredSize = new Dimension( 20, 20 )
      }

      overlayable.addOverlayComponent( progressPanel )
      progressPanel.start()
      overlayable.setOverlayVisible( true )

      val thread = new Thread() {
        override def run(): Unit = {
          super.run()

          UncaughtExceptionHandler( model.getLabel )

          val annotations = model.update()

          val ( panel, _table ) =
            SpecificationComputedComponent.createDerivedTreeTablePanel( model.computedDerivedTree, model )
          _table.addMouseListener(
            DoubleClickMouseListener.createAbstractTreeNodeInfoDoubleClickMouseListener( _table ) )
          HyperlinkTableCellValueEditorRenderer.addRenderer4AbstractTreeNodeInfo( _table )

          SwingUtilities.invokeLater( new Runnable() {
            override def run(): Unit = {
              model.fireTableDataChanged()

              treePanel.add( panel )

              overlayable.setOverlayVisible( false )
              overlayable.getOverlayComponents foreach {
                case comp: InfiniteProgressPanel =>
                  comp.stop()
                case _ =>
                  ()
              }

              _tables.put( row, _table )
              _tableModels.put( row, model )

              _table.expandAll()
              TableUtils.autoResizeAllColumns( _table )

              _htable.doLayout()

              val validationDataResults = ValidationAnnotation.toMagicDrawValidationDataResults(
                title = model.getLabel,
                validationAnnotations = annotations,
                postSessionActions = Nil )
              import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
              model.getProject.showMDValidationDataResultsIfAny( validationDataResults )
            }
          } )

        }
      }
      thread.start()
      overlayable
    }

    def createChildTreeComponentOrig
    ( _htable: HierarchicalTable, model: DerivedPropertyComputedTreeInfo, row: Int )
    : Component = {

      val ( panel, _table ) =
        SpecificationComputedComponent.createDerivedTreeTablePanel( model.computedDerivedTree, model )
      _table.addMouseListener(
        DoubleClickMouseListener.createAbstractTreeNodeInfoDoubleClickMouseListener( _table ) )
      HyperlinkTableCellValueEditorRenderer.addRenderer4AbstractTreeNodeInfo( _table )

      val treeLikeHierarchicalPanel = new TreeLikeHierarchicalPanel(
        SpecificationComputedComponent.FitScrollPane( _table ) )

      treeLikeHierarchicalPanel.setBackground( _table.getMarginBackground )
      val overlayable = new DefaultOverlayable( treeLikeHierarchicalPanel )

      val progressPanel = new InfiniteProgressPanel() {
        override def getPreferredSize = new Dimension( 20, 20 )
      }

      overlayable.addOverlayComponent( progressPanel )
      progressPanel.start()
      overlayable.setOverlayVisible( true )
      _tables.put( row, _table )
      _tableModels.put( row, model )
      val thread = new Thread() {
        override def run(): Unit = {
          super.run()

          UncaughtExceptionHandler( model.getLabel )
          val annotations = model.update()

          SwingUtilities.invokeLater( new Runnable() {
            override def run: Unit = {
              model.fireTableDataChanged()
              val internalTable = _tables.get( row )
              SpecificationComputedComponent.getTreeLikeHierarchicalPanelAncestorOfTable( internalTable ) match {
                case None => ()
                case Some( p ) =>
                  val overlayable = OverlayableUtils.getOverlayable( p )
                  overlayable.setOverlayVisible( false )
                  overlayable.getOverlayComponents map {
                    case comp: InfiniteProgressPanel =>
                      comp.stop()
                    case _ =>
                      ()
                  }
              }
              internalTable match {
                case None =>
                  ()
                case Some( t: TreeTable ) =>
                  t.setModel( _tableModels( row ) )
                  t.expandAll()
                  TableUtils.autoResizeAllColumns( t )
                case Some( t: JTable ) =>
                  t.setModel( _tableModels( row ) )
                  TableUtils.autoResizeAllColumns( t )
              }

              _htable.doLayout()

              val validationDataResults = ValidationAnnotation.toMagicDrawValidationDataResults(
                title = model.getLabel,
                validationAnnotations = annotations,
                postSessionActions = Nil )

              import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
              model.getProject.showMDValidationDataResultsIfAny( validationDataResults )
            }
          } )

        }
      }
      thread.start()
      overlayable
    }

    def createChildTableComponent
    ( _htable: HierarchicalTable,
      model: AbstractDisposableTableModel with AbstractTableModel,
      row: Int )
    : Component = {
      val emptyTableModel = StyleTableModel()
      val sortableTable = new SortableTable( emptyTableModel ) {
        override def scrollRectToVisible( aRect: Rectangle ) =
          SpecificationComputedComponent.scrollRectToVisible( this, aRect )
      }
      _group.add( sortableTable.getSelectionModel )

      val popupMenuInstaller = new TableHeaderPopupMenuInstaller( sortableTable )
      popupMenuInstaller.addTableHeaderPopupMenuCustomizer( new GroupTablePopupMenuCustomizer() )

      val stable = SearchableUtils.installSearchable( sortableTable )
      stable.setWildcardEnabled( true )
      stable.setFromStart( true )
      stable.setCountMatch( true )
      stable.setHideSearchPopupOnEvent( true )

      sortableTable
        .setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS )
      sortableTable
        .setSelectionMode( ListSelectionModel.SINGLE_SELECTION )
      sortableTable
        .addMouseListener(
          DoubleClickMouseListener
          .createAbstractTreeNodeInfoDoubleClickMouseListener( sortableTable ) )
      HyperlinkTableCellValueEditorRenderer.addRenderer4AbstractTreeNodeInfo( sortableTable )

      val treeLikeHierarchicalPanel =
        new TreeLikeHierarchicalPanel( SpecificationComputedComponent.FitScrollPane( sortableTable ) )
      treeLikeHierarchicalPanel.setBackground( sortableTable.getMarginBackground )
      val overlayable = new DefaultOverlayable( treeLikeHierarchicalPanel )

      val progressPanel = new InfiniteProgressPanel() {
        override def getPreferredSize = new Dimension( 20, 20 )
      }

      overlayable.addOverlayComponent( progressPanel )
      progressPanel.start()
      overlayable.setOverlayVisible( true )
      _tables.put( row, sortableTable )
      _tableModels.put( row, model )
      val thread = new Thread() {
        override def run(): Unit = {
          super.run()

          UncaughtExceptionHandler( model.getLabel )
          val annotations = model.update()
          model.fireTableDataChanged()

          SwingUtilities.invokeLater( new Runnable() {
            override def run(): Unit = {
              val internalTable = _tables.get( row )
              require( internalTable.isDefined )
              SpecificationComputedComponent.getTreeLikeHierarchicalPanelAncestorOfTable( internalTable ) match {
                case None =>
                  ()
                case Some( panel ) =>
                  val overlayable = OverlayableUtils.getOverlayable( panel )
                  overlayable.setOverlayVisible( false )
                  overlayable.getOverlayComponents map {
                    case comp: InfiniteProgressPanel =>
                      comp.stop()
                    case _ =>
                      ()
                  }
              }
              internalTable match {
                case None =>
                  ()
                case Some( t: TreeTable ) =>
                  t.setModel( _tableModels( row ) )
                  t.expandAll()
                  TableUtils.autoResizeAllColumns( t )
                case Some( t: JTable ) =>
                  t.setModel( _tableModels( row ) )
                  TableUtils.autoResizeAllColumns( t )
              }

              _htable.doLayout()

              val validationDataResults = ValidationAnnotation.toMagicDrawValidationDataResults(
                title = model.getLabel,
                validationAnnotations = annotations,
                postSessionActions = Nil )

              import gov.nasa.jpl.dynamicScripts.magicdraw.validation.internal.MDValidationAPIHelper._
              Application.getInstance.getProject.showMDValidationDataResultsIfAny( validationDataResults )
            }
          } )

        }
      }
      thread.start()
      overlayable
    }

    override def destroyChildComponent
    ( table: HierarchicalTable, component: Component, row: Int ) =
      JideSwingUtilities.getFirstChildOf( classOf[JTable], component ) match {
        case t: JTable =>
          _group.remove( t.getSelectionModel )
        case _ =>
          ()
      }
  }
}

object SpecificationComputedComponent {

  def getTreeLikeHierarchicalPanelAncestorOfTable
  ( table: Option[JTable] )
  : Option[TreeLikeHierarchicalPanel] =
  table.flatMap { jTable =>
    getTreeLikeHierarchicalPanelAncestorOfContainer( Option( jTable.getParent ) )
  }

  def getTreeLikeHierarchicalPanelAncestorOfContainer
  ( c: Option[Container] ): Option[TreeLikeHierarchicalPanel] =
    c match {
      case None                                     => None
      case Some( panel: TreeLikeHierarchicalPanel ) => Some( panel )
      case Some( cc )                               => getTreeLikeHierarchicalPanelAncestorOfContainer( Option( cc.getParent ) )
    }

  def collectParentsUpTo( c: Component, top: Component ): List[Component] =
    if ( c == top ) Nil
    else c :: collectParentsUpTo( c.getParent(), top )

  def getHierarchicalTableContainer( c: Component ): Option[HierarchicalTable] = c match {
    case null                 => None
    case h: HierarchicalTable => Some( h )
    case cp: Component        => getHierarchicalTableContainer( cp.getParent )
  }

  def getHierarchicalTableMainViewportAncestor( c: Component ): Option[JViewport] = c match {
    case null          => None
    case j: JViewport  => Some( j )
    case cp: Component => getHierarchicalTableMainViewportAncestor( cp.getParent )
  }

  def getHierarchicalTableMainViewport( c: Component ): Option[Either[Component, JViewport]] = c match {
    case null =>
      None
    case j: JViewport =>
      if ( null != j.getClientProperty( "HierarchicalTable.mainViewport" ) )
        Some( Right( j ) )
      else
        Some( Left( j ) )
    case c: Component =>
      Some( Left( c ) )
  }

  def scrollRectToVisible( component: Component, aRect: Rectangle ): Unit =
    scrollRectToVisible( component.getParent, aRect, component.getX, component.getY )

  def scrollRectToVisible( parent: Component, aRect: Rectangle, dx: Int, dy: Int ): Unit =
    getHierarchicalTableMainViewport( parent ) match {
      case None => ()
      case Some( Left( aParent ) ) =>
        val bounds = aParent.getBounds
        scrollRectToVisible( aParent.getParent, aRect, dx + bounds.x, dy + bounds.y )
      case Some( Right( aMainParent ) ) =>
        aRect.x += dx
        aRect.y += dy
        aMainParent.scrollRectToVisible( aRect )
        aRect.x -= dx
        aRect.y -= dy
    }

  case class FitScrollPane( view: Component ) extends JScrollPane( view ) with ComponentListener {

    initScrollPane

    def initScrollPane(): Unit = {
      setBorder( BorderFactory.createLineBorder( Color.GRAY ) )
      setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER )
      setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER )
      getViewport().getView().addComponentListener( this )
      removeMouseWheelListeners
      ()
    }

    // remove MouseWheelListener as there is no need for it in FitScrollPane.
    def removeMouseWheelListeners(): Unit =
      getMouseWheelListeners() foreach ( removeMouseWheelListener( _ ) )

    override def updateUI(): Unit = {
      super.updateUI
      removeMouseWheelListeners
    }

    override def componentMoved( e: ComponentEvent ) = ()
    override def componentShown( e: ComponentEvent ) = ()
    override def componentHidden( e: ComponentEvent ) = ()

    override def componentResized( e: ComponentEvent ) = {
      val h = getSize().height
      val ph = getPreferredSize.height
      val nh = if ( ph > h ) ph else h
      setSize( getSize().width, nh )
    }

    override def getPreferredSize = {
      getViewport.setPreferredSize( getViewport.getView.getPreferredSize )
      super.getPreferredSize()
    }
  }

  case class FitScrollPaneToAncestor( panel: JPanel, treeTable: TreeTable, otherPanels: JPanel* ) extends JScrollPane( treeTable ) with ComponentListener {

    initScrollPane

    def initScrollPane() = {
      setBorder( BorderFactory.createLineBorder( Color.GRAY ) )
      setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED )
      setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED )
      getViewport().getView().addComponentListener( this )
    }

    override def componentMoved( e: ComponentEvent ) = ()
    override def componentShown( e: ComponentEvent ) = ()
    override def componentHidden( e: ComponentEvent ) = ()

    override def componentResized( e: ComponentEvent ) = {
      fitTreeTableToAncestorViewport( panel, treeTable, otherPanels: _* )
    }

  }

  def fitTreeTableToAncestorViewport( panel: JPanel, treeTable: TreeTable, otherPanels: JPanel* ): Unit = {
    SpecificationComputedComponent.getTreeLikeHierarchicalPanelAncestorOfTable( Some( treeTable ) ) match {
      case None =>
        ()
      case Some( panel ) =>
        ( SpecificationComputedComponent.getHierarchicalTableMainViewportAncestor( panel ), SpecificationComputedComponent.getHierarchicalTableContainer( treeTable ) ) match {
          case ( None, _ ) =>
            ()
          case ( _, None ) =>
            ()
          case ( Some( viewport ), Some( htable ) ) =>
            val ancestors = SpecificationComputedComponent.collectParentsUpTo( treeTable, viewport )
            val children = for {
              row <- 0 until htable.getRowCount()
              child = htable.getChildComponentAt( row )
              if ( child != null )
              if ( ancestors contains child )
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
                val adjustedHeight = ( viewHeight /: rowsAbove ) { case ( hi, ri ) => hi - htable.getActualRowHeight( ri ) }
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

  def getTreeRowExtent[T <: ExpandableRow]( row: T )( implicit tag: ClassTag[T] ): Set[T] =
    row.getChildren() match {
      case null => Set()
      case rows =>
        val childRows = rows flatMap { child =>
          child match {
            case childRow: T => Some( childRow )
            case _           => None
          }
        } toSet;
        val childRowExtents = childRows flatMap ( getTreeRowExtent( _ ) )
        val extent = childRows ++ childRowExtents
        extent
    }

  def getDerivedPropertyComputedTreeRowValidationAnnotations( row: DerivedPropertyComputedTreeRow ): Set[ValidationAnnotation] = {
    val infoAnnotations = row.info.getAnnotations.toSet
    val rowAnnotations = row.row.values flatMap ( _.getAnnotations.toSet )
    Set[ValidationAnnotation]() ++ infoAnnotations ++ rowAnnotations
  }
  
  def getDerivedPropertyComputedTreeRowElement( row: DerivedPropertyComputedTreeRow ): Set[Element] = {
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

  def createDerivedTreeTablePanel( derived: DynamicScriptsTypes.ComputedDerivedTree, model: TreeTableModel[DerivedPropertyComputedTreeRow] ): ( JPanel, TreeTable ) = {

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

    val filterableTreeTableModel = new FilterableTreeTableModel( filterField.getDisplayTableModel() )
    val sortableTreeTableModel = new SortableTreeTableModel( filterableTreeTableModel )

    var uniqueRows: Option[Int] = None
    var uniqueElements: Option[Int] = None
    var uniqueAnnotations: Option[Int] = None
    var uniqueTableSummary: String = ""
    def updateTableLabels(): Unit = {
      val totalExtent = uniqueRows match {
        case Some( extent ) => extent
        case None =>
          val topRows = model.getRows.toSet;
          val extent = topRows flatMap ( getTreeRowExtent( _ ) )
          uniqueRows = Some( extent.size )
          val elements = extent flatMap ( getDerivedPropertyComputedTreeRowElement( _ ) )
          uniqueElements = Some( elements.size )
          val annotations = extent flatMap ( getDerivedPropertyComputedTreeRowValidationAnnotations( _ ) )
          uniqueAnnotations = Some( annotations.size )
          uniqueTableSummary = s"[Total of ${uniqueElements.get} unique elements; ${uniqueAnnotations.get} unique validation annotations]"
          extent.size
      }

      val visibleRows = filterableTreeTableModel.getRowCount
      val totalRows = filterableTreeTableModel.getActualModel.getRowCount

      if ( visibleRows == totalRows ) {
        tableTitleBorder.setTitle( s"Filtered /${derived.name.hname} (All $totalRows rows with $totalExtent unique values. $uniqueTableSummary" )
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
        tableTitleBorder.setTitle( s"Filtered /${derived.name.hname} ($visibleRows visible rows with $visibleExtent unique values shown after applying filters to all $totalRows rows with $totalExtent unique values) $uniqueTableSummary" )
        StyledLabelBuilder.setStyledText(
          _filterTimesLabel,
          s"Filters result in {$visibleRows:f:blue} visible rows with {$visibleExtent:f:blue} unique values from a total of $totalRows rows with $totalExtent unique values" )
      }
    }

    sortableTreeTableModel.setDefaultSortableOption( SortableTreeTableModel.SORTABLE_LEAF_LEVEL )
    sortableTreeTableModel.setSortableOption( 0, SortableTreeTableModel.SORTABLE_ROOT_LEVEL )
    sortableTreeTableModel.addTableModelListener( new TableModelListener() {
      def tableChanged( e: TableModelEvent ): Unit =
        if ( e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW )
          updateTableLabels
    } )
    sortableTreeTableModel.addIndexChangeListener( new IndexChangeListener() {
      def indexChanged( event: IndexChangeEvent ): Unit = {
        ( event.getSource, event.getType ) match {
          case ( fmodel: FilterableTreeTableModel[_], IndexChangeEvent.INDEX_CHANGED_EVENT ) =>
            updateTableLabels
          case ( x, y ) =>
            ()
        }
      }
    } )

    val treeTable = new TreeTable( sortableTreeTableModel ) {
      override def scrollRectToVisible( aRect: Rectangle ) = SpecificationComputedComponent.scrollRectToVisible( this, aRect )

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
        if ( e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW ) {
          model.expandAll
          updateTableLabels
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
    val scrollPane = FitScrollPaneToAncestor( panel, treeTable, quickSearchPanel, infoPanel )
    tablePanel.add( scrollPane )

    panel.add( quickSearchPanel, BorderLayout.BEFORE_FIRST_LINE )
    panel.add( tablePanel )
    panel.add( infoPanel, BorderLayout.AFTER_LAST_LINE )

    createTableHeaderPopupMenuInstaller( panel, treeTable, updateTableLabels, quickSearchPanel, infoPanel )

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

      override def customizeStyledLabel( table: JTable, value: Object, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int ): Unit = {
        super.customizeStyledLabel( table, value, isSelected, hasFocus, row, column )
        val r = model.getRowAt( row )
        val style = treeTable.getCellStyleAt( row, column )
        val validationAnnotations = r.row.values flatMap ( _.getAnnotations )
        val validationSeverities = validationAnnotations map ( _.annotation.getSeverity ) toSet;
        if ( validationSeverities.nonEmpty ) {
          val lowestSeverity = validationSeverities.toList.sorted( MDValidationAPIHelper.SEVERITY_LEVEL_ORDERING ).head
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
        updateTableLabels
        fitTreeTableToAncestorViewport( panel, treeTable, quickSearchPanel, infoPanel )
      }

      def treeCollapsed( event: TreeExpansionEvent ): Unit = {
        updateTableLabels
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
          case node: AbstractTreeNodeInfo => node.identifier
          case _                          => ""
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

  def createTableHeaderPopupMenuInstaller(
    panel: JPanel, treeTable: TreeTable,
    callback: => Unit,
    otherPanels: JPanel* ): TableHeaderPopupMenuInstaller = {

    val installer = new TableHeaderPopupMenuInstaller( treeTable ) {

      override def customizeMenuItems( header: JTableHeader, popup: JPopupMenu, clickingColumn: Int ): Unit = {
        super.customizeMenuItems( header, popup, clickingColumn )

        val expandAll = new JMenuItem( new AbstractAction( "Expand All" ) {
          def actionPerformed( ev: ActionEvent ): Unit = {
            treeTable.expandAll
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
                  treeRows.head.getTreeTableModel.asInstanceOf[TreeTableModel[DefaultExpandableRow]].expandRows( treeRows, true )
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
            treeTable.getModel() match {
              case ttm: ITreeTableModel[_] =>
                val rows = treeTable.getSelectedRows
                if ( rows.isEmpty ) {
                  treeTable.collapseLastLevel
                }
                else {
                  val treeRows = rows.toList flatMap { rowIndex =>
                    ttm.getRowAt( rowIndex ) match {
                      case treeRow: DefaultExpandableRow => Some( treeRow )
                      case _                             => None
                    }
                  }
                  treeRows.head.getTreeTableModel.asInstanceOf[TreeTableModel[DefaultExpandableRow]].collapseRows( treeRows, true )
                }
                callback
              case _ => ()
            }
        } )

        val fitTable = new JMenuItem( new AbstractAction( "Fit Table" ) {
          def actionPerformed( ev: ActionEvent ): Unit = {
            fitTreeTableToAncestorViewport( panel, treeTable, otherPanels: _* )
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

}