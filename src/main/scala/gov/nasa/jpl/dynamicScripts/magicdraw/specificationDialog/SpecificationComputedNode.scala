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

package gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog

import java.beans.PropertyChangeEvent
import javax.swing.table.AbstractTableModel

import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.ISpecificationNode
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.AbstractHierarchicalDisposableTableModel

import scala.Predef.{require, String}

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