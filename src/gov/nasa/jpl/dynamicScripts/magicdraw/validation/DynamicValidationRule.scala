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
package gov.nasa.jpl.dynamicScripts.magicdraw.validation

import java.lang.Class

import com.nomagic.magicdraw.validation.ElementValidationRuleImpl
import com.nomagic.magicdraw.validation.SmartListenerConfigurationProvider
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.uml2.ext.jmi.smartlistener.SmartListenerConfig

import scala.collection.immutable._
import scala.collection.JavaConverters._
import scala.{Unit}
/**
 * For now, this MagicDraw Validation Rule does nothing; it is intended to be the binary implementation
 * of MagicDraw Validation constraints that are used in Dynamic Scripts Validation. 
 * 
 * @todo Consider extending the Dynamic Scripts DSL with a "DynamicValidation" type
 * This "DynamicValidation" type would specify:
 * - a DynamicScript to be invoked for initializing & executing the validation
 * - a Constraint element that would be associated to a "DynamicValidation" 
 *
 * In MD's validation framework, the DynamicValidationRule's init() and run() methods 
 * are invoked with a validation Constraint which would serve as the key for a lookup of a "DynamicValidation" object
 * that would in turn specify the tool-specific methods to invoke (e.g., init() and run() in the case of MD).
 *
 * Before going further with this idea, check with Eclipse/Papyrus if there is enough commonality in the concept of
 * model validation such that the "DynamicValidation" type described above could fit the validation architecture
 * of both MD and Eclipse/Papyrus.
 *
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicValidationRule() 
extends ElementValidationRuleImpl()
with SmartListenerConfigurationProvider {
  
  def init(project: Project, constraint: Constraint): Unit = {
    ()
  }
  
  def run(project: Project, constraint: Constraint, elements: java.util.Collection[_ <: Element]): java.util.Set[Annotation] = {
    Set().asJava
  }
  
  def dispose: Unit = {
    ()
  }
  
  def getListenerConfigurations: java.util.Map[Class[_ <: Element], java.util.Collection[SmartListenerConfig]] = {
     Map().asJava
   }
}