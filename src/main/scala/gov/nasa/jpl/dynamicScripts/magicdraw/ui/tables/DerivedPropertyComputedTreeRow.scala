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

package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.lang.{Class, Comparable, Object}

import scala.collection.immutable._
import scala.{Int, StringContext}
import scala.Predef.{classOf, require, String}

import com.jidesoft.grid.DefaultExpandableRow

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
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
    val value: Object = getValueAt( columnIndex )
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