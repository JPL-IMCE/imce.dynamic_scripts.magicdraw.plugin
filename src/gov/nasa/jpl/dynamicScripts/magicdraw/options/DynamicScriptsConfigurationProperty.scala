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
package gov.nasa.jpl.dynamicScripts.magicdraw.options

import com.nomagic.magicdraw.properties.StringProperty

import java.lang.Object

import gov.nasa.jpl.dynamicScripts.magicdraw.options.resources.DynamicScriptsResources

import scala.collection.immutable._
import scala.{Boolean,StringContext}
import scala.Predef.{refArrayOps, String}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsConfigurationProperty
(id: String, value: Object, multiline: Boolean)
  extends StringProperty(id, value, multiline) {

  override def getValueStringRepresentation: String =
    getString

  def getDynamicScriptConfigurationFiles: List[String] =
    getString match {
      case null          => List()
      case ""            => List()
      case files: String => files.split( "\n" ).toList
    }
  
  override def getName: String =
    DynamicScriptsResources.getString(DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_NAME)
    
  override def getGroup: String =
    DynamicScriptsResources.getString(DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_GROUP)
    
  override def getDescriptionID: String =
    DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_DESC
    
  override def getDescription: String =
    DynamicScriptsResources.getString(DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_DESC)

}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsConfigurationProperty {
  
   def getDynamicScriptConfigurationFiles(p: StringProperty): List[String] =
    p.getString match {
      case null          => List()
      case ""            => List()
      case files: String => files.split( "\n" ).toList
    }
}