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