package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import scala.language.postfixOps
import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel
import com.jidesoft.grid.CellPainter
import com.jidesoft.grid.CellStyle
import com.jidesoft.grid.StyleModel
import com.jidesoft.swing.OverlayableIconsFactory
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import com.nomagic.magicdraw.annotation.Annotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
trait AbstractDisposableTableModel 
extends Comparable[AbstractDisposableTableModel] 
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
      
      case node: AbstractTreeNodeInfo =>
        node.getAnnotations match {
          case Seq() => ()
          case validationAnnotations => 
            val validationSeverities = validationAnnotations map ( _.annotation.getSeverity ) toSet
            val lowestSeverity = validationSeverities.toList sorted ( ValidationAnnotation.SEVERITY_LEVEL_ORDERING ) head
            val lowestAnnotations = validationAnnotations filter ( _.annotation.getSeverity == lowestSeverity )
            require( lowestAnnotations.nonEmpty )
            
            val annotationIcon = lowestAnnotations find ( null != _.annotation.getSeverityImageIcon ) match {
              case None => ValidationAnnotation.severity2Icon(lowestSeverity) 
              case Some( a ) => Some( Annotation.getIcon( a.annotation ) )
            }
            
            annotationIcon match {
              case None => ()
              case Some( icon ) => icon.paintIcon( component, g, cellRect.x + cellRect.width - icon.getIconWidth() - 1, cellRect.y )
            }
        } 
        
        case _ =>
          ()
      }
  } )

}