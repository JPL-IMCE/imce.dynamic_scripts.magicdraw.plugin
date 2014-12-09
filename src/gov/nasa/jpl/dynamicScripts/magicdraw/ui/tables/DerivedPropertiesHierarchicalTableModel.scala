package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import scala.collection.JavaConversions.seqAsJavaList

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertiesHierarchicalTableModel[T <: AbstractDisposableTableModel]
    ( val e: Element, 
      val computedProperties: Seq[T], 
      override val columns: java.util.Vector[String] = new java.util.Vector[String](DerivedPropertiesHierarchicalTableModel.COLUMNS) )
    extends AbstractHierarchicalDisposableTableModel(AbstractHierarchicalDisposableTableModel.toTable( computedProperties ), columns) {

  override def dispose = ()
  
  override def update = Seq()
  
  override def getLabel = ""
  
  override def getValueAt( row: Int, column: Int): Object = {   
    require (0 <= row && row < computedProperties.size)
    require (0 <= column)
    if (0 != column) null
    else computedProperties(row).getLabel
  }
  
  override def getChildValueAt( row: Int ): Object = { 
    require (0 <= row && row < computedProperties.size)
    computedProperties(row)
  }
}

object DerivedPropertiesHierarchicalTableModel {
  
  val COLUMNS = Seq( "Computed Derived Property" )  
}
