package gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog

import java.beans.PropertyChangeEvent
import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.ISpecificationNode
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.AbstractHierarchicalDisposableTableModel
import javax.swing.table.AbstractTableModel

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class SpecificationComputedNode[T <: AbstractHierarchicalDisposableTableModel with AbstractTableModel](
  ID: String, label: String, e: Element, table: T )
  extends ISpecificationNode {

  override def getID= ID
  override def getIcon = DynamicScriptsPlugin.getInstance.getJPLSmallIcon
  override def getText = label
  override def dispose = table.dispose
  override def propertyChanged( element: Element, event: PropertyChangeEvent ) = ()
  override def updateNode = false

  override def createSpecificationComponent( element: Element ) = {
    require( e == element )
    SpecificationComputedComponent[T]( table, this )
  }

}