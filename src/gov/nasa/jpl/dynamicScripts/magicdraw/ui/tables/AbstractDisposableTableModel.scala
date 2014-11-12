package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle

import javax.swing.table.DefaultTableModel

import com.jidesoft.grid.CellPainter
import com.jidesoft.grid.CellStyle
import com.jidesoft.grid.StyleModel
import com.jidesoft.swing.OverlayableIconsFactory

import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
abstract class AbstractDisposableTableModel(
  val table: java.util.Vector[java.util.Vector[String]],
  val columns: java.util.Vector[String] )
  extends DefaultTableModel( table, columns )
  with Comparable[AbstractDisposableTableModel]
  with StyleModel {

  def dispose: Unit

  def update: Seq[ValidationAnnotation]

  def getLabel: String

  override def compareTo( o: AbstractDisposableTableModel ): Int =
    getLabel.compareTo( o.getLabel )

  override def getCellStyleAt( rowIndex: Int, columnIndex: Int ): CellStyle = AbstractDisposableTableModel.COMPUTED_CELL_STYLE

  override def isCellStyleOn: Boolean = true

}

object AbstractDisposableTableModel {

  val COMPUTED_CELL_STYLE = new CellStyle()

  COMPUTED_CELL_STYLE.setOverlayCellPainter( new CellPainter() {

    override def paint( g: Graphics, component: Component, row: Int, column: Int, cellRect: Rectangle, value: Object ): Unit =
      value match {
      
        case ai: AnnotationNodeInfo =>
          val iconKind = if ( ai.isError ) OverlayableIconsFactory.ERROR
          else if ( ai.isWarning ) OverlayableIconsFactory.ATTENTION
          else if ( ai.isInfo ) OverlayableIconsFactory.CORRECT
          else OverlayableIconsFactory.QUESTION
          val icon = OverlayableIconsFactory.getImageIcon( iconKind )
          icon.paintIcon( component, g, cellRect.x + cellRect.width - icon.getIconWidth() - 1, cellRect.y )

        case _ =>
          ()
      }
  } )

}