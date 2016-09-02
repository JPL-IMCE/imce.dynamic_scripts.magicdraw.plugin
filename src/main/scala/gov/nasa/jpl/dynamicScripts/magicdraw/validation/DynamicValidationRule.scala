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