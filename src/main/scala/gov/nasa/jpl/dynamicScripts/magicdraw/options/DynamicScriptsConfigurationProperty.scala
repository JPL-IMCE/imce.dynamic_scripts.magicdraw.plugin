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

package gov.nasa.jpl.dynamicScripts.magicdraw.options

import com.nomagic.magicdraw.properties.StringProperty

import java.lang.Object

import gov.nasa.jpl.dynamicScripts.magicdraw.options.resources.DynamicScriptsResources

import scala.collection.immutable._
import scala.Boolean
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