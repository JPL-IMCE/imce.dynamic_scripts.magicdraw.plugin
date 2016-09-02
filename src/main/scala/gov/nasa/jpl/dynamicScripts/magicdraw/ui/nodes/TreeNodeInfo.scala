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

package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import java.lang.Object

import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

import scala.collection.immutable._
import scala.Boolean
import scala.Predef.String
/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class TreeNodeInfo( 
    override val identifier: String, 
    val nested: Seq[(AbstractTreeNodeInfo, Map[String, AbstractTreeNodeInfo])] = Seq(),
    val annotations: Seq[ValidationAnnotation] = Seq() )
extends AbstractTreeNodeInfo( identifier ) {
  
  val compareKey = identifier
  
  def getAnnotations = annotations
}

object TreeNodeInfo {  
  
  def isTable( o: Object ): Boolean = o match {
    case _: TreeNodeInfo => true
    case _ => false
  }
}