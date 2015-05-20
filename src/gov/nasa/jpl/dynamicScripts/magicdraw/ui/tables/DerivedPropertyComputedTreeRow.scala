/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.awt.event.ActionEvent
import scala.language.existentials
import scala.util.Failure
import scala.util.Success
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import com.jidesoft.grid.TreeTableModel
import com.jidesoft.grid.AbstractExpandableRow
import com.jidesoft.grid.DefaultExpandable
import com.jidesoft.grid.DefaultExpandableRow
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.TreeNodeInfo

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedTreeRow( info: AbstractTreeNodeInfo,
                                           row: Map[String, AbstractTreeNodeInfo],
                                           children: Seq[DerivedPropertyComputedTreeRow],
                                           computedDerivedTree: ComputedDerivedTree )
  extends DefaultExpandableRow
  with Comparable[DerivedPropertyComputedTreeRow] {

  require( computedDerivedTree.columnValueTypes.isDefined, s"A DerivedPropertyComputedTree must have explicitly-specified column value types!" )
  require( computedDerivedTree.columnValueTypes.get.nonEmpty, s"A DerivedPropertyComputedTree must have at least 1 column value type!" )

  val columnValueTypes = computedDerivedTree.columnValueTypes.get
  
  def getValueAt( columnIndex: Int ): Object = 
    if (0 == columnIndex)
      info
    else {
      require( 0 < columnIndex && columnIndex <= columnValueTypes.size )
      val column = columnValueTypes( columnIndex-1 ).key.sname
      if ( row.contains( column ) )
        row( column )
      else
        null
    }

  override def getCellClassAt( columnIndex: Int ): Class[_] = {
    val value = getValueAt( columnIndex )
    value match {
      case x: TreeNodeInfo =>
        classOf[DerivedPropertyComputedTreeRow]
      case x =>
        classOf[Object]
    }
  }

  override def compareTo( o: DerivedPropertyComputedTreeRow ): Int =
    info.compareTo( o.info )
}