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

import java.lang.Object
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.immutable._
import scala.{Int, StringContext}
import scala.Predef.{require, String}

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertiesHierarchicalTableModel[T <: AbstractDisposableTableModel]
    ( val e: Element, 
      val computedProperties: Seq[T], 
      override val columns: java.util.Vector[String] = new java.util.Vector[String](DerivedPropertiesHierarchicalTableModel.COLUMNS) )
    extends AbstractHierarchicalDisposableTableModel(AbstractHierarchicalDisposableTableModel.toTable( computedProperties ), columns) {

  override def dispose() = ()
  
  override def update() = Seq()
  
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