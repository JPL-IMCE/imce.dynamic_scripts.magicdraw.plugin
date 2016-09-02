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

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

import scala.collection.immutable.Seq
import scala.Predef.String

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class ReferenceNodeInfo
( override val identifier: String, val e: Element, annotations: Seq[ValidationAnnotation] = Seq() )
extends AbstractTreeNodeInfo( identifier ) {
  
  val compareKey: String = identifier + e.getHumanType + e.getID

  def getAnnotations = annotations
}