package gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog

import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.beans.PropertyChangeEvent

import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JViewport
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

import scala.Left
import scala.Right
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.postfixOps

import com.jidesoft.grid.AutoFilterTableHeader
import com.jidesoft.grid.HierarchicalTable
import com.jidesoft.grid.HierarchicalTableComponentFactory
import com.jidesoft.grid.ListSelectionModelGroup
import com.jidesoft.grid.SortableTable
import com.jidesoft.grid.StyleModel
import com.jidesoft.grid.TreeLikeHierarchicalPanel
import com.jidesoft.swing.DefaultOverlayable
import com.jidesoft.swing.InfiniteProgressPanel
import com.jidesoft.swing.JideSwingUtilities
import com.jidesoft.swing.OverlayableUtils
import com.jidesoft.swing.SearchableUtils
import com.nomagic.magicdraw.ui.dialogs.specifications.ISpecificationComponent
import com.nomagic.magicdraw.ui.dialogs.specifications.SpecificationDialogManager
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.MagicDrawValidationDataResults
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes._
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.AbstractDisposableTableModel
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.AbstractHierarchicalDisposableTableModel
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class SpecificationComputedComponent[T <: AbstractHierarchicalDisposableTableModel](
  model: T, node: SpecificationComputedNode[T] )
  extends ISpecificationComponent {

  override def propertyChanged( element: Element, event: PropertyChangeEvent ) = ()
  override def updateComponent = ()
  override def dispose = model.dispose

  val _group = new ListSelectionModelGroup()
  
  override def getComponent: JComponent = {
    val table = new HierarchicalTable( model )
    table.setAutoRefreshOnRowUpdate( true )
    table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION )
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
        case model: AbstractDisposableTableModel => createChildComponent( _table, model, row )
        case x                                   => throw new IllegalArgumentException( s" createChildComponent: unrecognized value: ${x}" )
      }

    def createChildComponent( _table: HierarchicalTable, model: AbstractDisposableTableModel, row: Int ): Component = {
      val emptyTableModel = StyleTableModel()
      val sortableTable = new SortableTable( emptyTableModel ) {
        override def scrollRectToVisible( aRect: Rectangle ) = SpecificationComputedComponent.scrollRectToVisible( this, aRect )
      }
      //sortableTable.setBackground(BG2);
      _group.add( sortableTable.getSelectionModel() )

      val stable = SearchableUtils.installSearchable( sortableTable )
      stable.setWildcardEnabled( true )
      stable.setFromStart( true )
      stable.setCountMatch( true )
      stable.setHideSearchPopupOnEvent( true )

      sortableTable.setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS )
      sortableTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION )
      sortableTable.addMouseListener( new MouseListener() {

        override def mouseClicked( ev: MouseEvent ): Unit = {
          if ( ev.getClickCount() == 2 && ev.getButton() == MouseEvent.BUTTON1 ) {
            val source = ev.getSource()
            require( sortableTable.equals( source ) )
            val row = sortableTable.rowAtPoint( ev.getPoint() )
            val col = sortableTable.columnAtPoint( ev.getPoint() )

            val m = SpecificationDialogManager.getManager()
            val modifiers = ev.getModifiersEx
            val o = sortableTable.getModel().getValueAt( row, col )
            o match {
              case n: AnnotationNodeInfo =>
                n.getAnnotation.getTarget match {
                  case e: Element =>
                    if ( ( modifiers & InputEvent.SHIFT_DOWN_MASK ) == InputEvent.SHIFT_DOWN_MASK ) {
                      JOptionPane.showMessageDialog( sortableTable,
                        s"${e.getHumanType}: '${e.getHumanName}' =>\n${n.getAnnotation.getText}",
                        n.getAnnotationKind,
                        n.getAnnotationMessageKind,
                        DynamicScriptsPlugin.getInstance.getJPLSmallIcon )
                    }
                    else {
                      m.editSpecification( e )
                    }
                  case _ => ()
                }
                
              case n: ReferenceNodeInfo =>
                m.editSpecification( n.e )
                
              case n: LabelNodeInfo =>
                ()
            }
          }
          ev.consume()
        }

        override def mousePressed( ev: MouseEvent ): Unit = ev.consume
        override def mouseReleased( ev: MouseEvent ): Unit = ev.consume
        override def mouseEntered( ev: MouseEvent ): Unit = ev.consume
        override def mouseExited( ev: MouseEvent ): Unit = ev.consume

      } )

      val treeLikeHierarchicalPanel = new TreeLikeHierarchicalPanel( SpecificationComputedComponent.FitScrollPane( sortableTable ) )
      treeLikeHierarchicalPanel.setBackground( sortableTable.getMarginBackground() )
      val overlayable = new DefaultOverlayable( treeLikeHierarchicalPanel )

      val progressPanel = new InfiniteProgressPanel() {
        override def getPreferredSize() = new Dimension( 20, 20 )
      }

      overlayable.addOverlayComponent( progressPanel )
      progressPanel.start()
      overlayable.setOverlayVisible( true )
      _tables.put( row, sortableTable )
      _tableModels.put( row, model )
      val thread = new Thread() {
        override def run: Unit = {
          super.run()

          val annotations = model.update
          model.fireTableDataChanged()

          SwingUtilities.invokeLater( new Runnable() {
            override def run: Unit = {
              val internalTable = _tables.get( row )
              require( internalTable.isDefined )
              SpecificationComputedComponent.getTreeLikeHierarchicalPanelAncestorOfTable( internalTable ) match {
                case None => ()
                case Some( panel ) =>
                  val overlayable = OverlayableUtils.getOverlayable( panel )
                  overlayable.setOverlayVisible( false )
                  overlayable.getOverlayComponents() map {
                    case comp: InfiniteProgressPanel => comp.stop
                    case _                           => ()
                  }
              }
              internalTable.get.setModel( _tableModels( row ) )
              _table.doLayout

              val validationDataResults = ValidationAnnotation.toMagicDrawValidationDataResults(
                title = model.getLabel,
                validationAnnotations = annotations,
                postSessionActions = Nil )
              MagicDrawValidationDataResults.showMDValidationDataResultsIfAny( validationDataResults )
            }
          } )
        }
      }
      thread.start()
      return overlayable
    }

    override def destroyChildComponent( table: HierarchicalTable, component: Component, row: Int ) =
      JideSwingUtilities.getFirstChildOf( classOf[JTable], component ) match {
        case t: JTable => _group.remove( t.getSelectionModel() )
        case _         => ()
      }
  }
}

object SpecificationComputedComponent {

  def getTreeLikeHierarchicalPanelAncestorOfTable( table: Option[JTable] ): Option[TreeLikeHierarchicalPanel] =
    table match {
      case None           => None
      case Some( jTable ) => getTreeLikeHierarchicalPanelAncestorOfContainer( Option( jTable.getParent ) )
    }

  def getTreeLikeHierarchicalPanelAncestorOfContainer( c: Option[Container] ): Option[TreeLikeHierarchicalPanel] =
    c match {
      case None                                     => None
      case Some( panel: TreeLikeHierarchicalPanel ) => Some( panel )
      case Some( cc )                               => getTreeLikeHierarchicalPanelAncestorOfContainer( Option( cc.getParent ) )
    }

  def getHierarchicalTableMainViewpoint( c: Component ): Option[Either[Component, JViewport]] = c match {
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
    scrollRectToVisible( component.getParent, aRect, component.getX(), component.getY() )

  def scrollRectToVisible( parent: Component, aRect: Rectangle, dx: Int, dy: Int ): Unit =
    getHierarchicalTableMainViewpoint( parent ) match {
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

    def initScrollPane = {
      setBorder( BorderFactory.createLineBorder( Color.GRAY ) )
      setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER )
      setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER )
      getViewport().getView().addComponentListener( this )
      removeMouseWheelListeners
    }

    // remove MouseWheelListener as there is no need for it in FitScrollPane.
    def removeMouseWheelListeners: Unit =
      getMouseWheelListeners() foreach ( removeMouseWheelListener( _ ) )

    override def updateUI: Unit = {
      super.updateUI
      removeMouseWheelListeners
    }

    override def componentMoved( e: ComponentEvent ) = ()
    override def componentShown( e: ComponentEvent ) = ()
    override def componentHidden( e: ComponentEvent ) = ()

    override def componentResized( e: ComponentEvent ) = setSize( getSize().width, getPreferredSize().height )

    override def getPreferredSize = {
      getViewport().setPreferredSize( getViewport().getView().getPreferredSize() )
      super.getPreferredSize()
    }
  }
}