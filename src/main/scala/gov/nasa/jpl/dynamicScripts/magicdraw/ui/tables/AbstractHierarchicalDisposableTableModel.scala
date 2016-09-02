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

import java.lang.{Class, Object}

import com.jidesoft.grid.HierarchicalTableModel

import scala.collection.immutable._
import scala.{Boolean, Int, Unit}
import scala.Predef.{classOf, String}

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
abstract class AbstractHierarchicalDisposableTableModel(
  override val table: java.util.Vector[java.util.Vector[String]],
  override val columns: java.util.Vector[String] )
  extends AbstractDefaultDisposableTableModel( table, columns )
  with HierarchicalTableModel {

  override def dispose(): Unit
   
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