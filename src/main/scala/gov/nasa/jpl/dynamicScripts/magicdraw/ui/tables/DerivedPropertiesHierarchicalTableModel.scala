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

import java.lang.Object
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.immutable._
import scala.Int
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