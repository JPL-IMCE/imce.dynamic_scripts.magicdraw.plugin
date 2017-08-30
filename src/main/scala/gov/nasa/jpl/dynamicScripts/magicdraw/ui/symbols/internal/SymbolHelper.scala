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

package gov.nasa.jpl.dynamicScripts.magicdraw.ui.symbols.internal

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package

import com.nomagic.magicdraw.uml.symbols.paths.AssociationView
import com.nomagic.magicdraw.uml.symbols.shapes.PackageView

import scala.{deprecated,Option}

@deprecated("", "")
class SymbolHelper {}

object SymbolHelper {

  def getAssociationOfView(av: AssociationView): Option[Association] =
    Option(av.getElement)

  def getPackageOfView(pv: PackageView): Option[Package] =
    Option(pv.getElement)

}