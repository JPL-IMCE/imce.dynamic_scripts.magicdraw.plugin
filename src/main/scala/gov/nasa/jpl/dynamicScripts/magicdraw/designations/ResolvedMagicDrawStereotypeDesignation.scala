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

package gov.nasa.jpl.dynamicScripts.magicdraw.designations

import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import com.nomagic.uml2.ext.magicdraw.mdprofiles._
import com.nomagic.uml2.impl.ElementsFactory

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._

import scala.language.existentials
import scala.util.Success
import scala.util.Try
import scala.{Boolean, None}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class ResolvedMagicDrawStereotypeDesignation(
  project: Project, d: StereotypedMetaclassDesignation,
  creator: ElementsFactory => Element,
  metaclass: java.lang.Class[_ <: Element],
  profile: Profile,
  stereotype: Stereotype ) extends MagicDrawStereotypeDesignation with ResolvedMagicDrawDesignation {
  val isResolved = true
  val resolutionError = None

  def designationMatches( e: Element ): Boolean =
    metaclass.isInstance( e ) && StereotypesHelper.hasStereotype( e, stereotype )

  def createElement( project: Project ): Try[Element] = {
    val e = creator( project.getElementsFactory )
    StereotypesHelper.addStereotype( e, stereotype )
    Success( e )
  }
}