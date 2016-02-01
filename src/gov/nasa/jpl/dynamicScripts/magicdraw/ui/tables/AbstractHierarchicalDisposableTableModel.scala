/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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