package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import com.jidesoft.grid.HierarchicalTableModel

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
abstract class AbstractHierarchicalDisposableTableModel(
  override val table: java.util.Vector[java.util.Vector[String]],
  override val columns: java.util.Vector[String] )
  extends AbstractDefaultDisposableTableModel( table, columns )
  with HierarchicalTableModel {

  override def dispose: Unit
   
  override def isCellEditable(row: Int, column: Int): Boolean = false
  
  override def hasChild(row: Int): Boolean = true
  
  override def isExpandable(row: Int): Boolean = true
  
  override def isHierarchical(row: Int): Boolean = true
  
  override def getColumnClass(columnIndex: Int): Class[_] = classOf[String]
  
  override def getChildValueAt(row: Int): Object
}

object AbstractHierarchicalDisposableTableModel {
  
  def toTable( rows: Seq[_ <: AbstractDisposableTableModel] ): java.util.Vector[java.util.Vector[String]] = {
    val jrows = new java.util.Vector[java.util.Vector[String]]( rows.size )
    rows foreach { row => 
      val jrow = new java.util.Vector[String](1)
      jrow.add( row.getLabel )
      jrows.add( jrow )
    }
    jrows
  }
}
