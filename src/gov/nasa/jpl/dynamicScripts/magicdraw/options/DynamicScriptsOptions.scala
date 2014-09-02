/**
 * License Terms
 *
 * Copyright (c) 2014, California
 * Institute of Technology ("Caltech").  U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *
 *  *   Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *  *   Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the
 *      distribution.
 *
 *  *   Neither the name of Caltech nor its operating division, the Jet
 *      Propulsion Laboratory, nor the names of its contributors may be
 *      used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import scala.language.implicitConversions
import scala.language.postfixOps

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.options.AbstractPropertyOptionsGroup
import com.nomagic.magicdraw.properties.NumberProperty
import com.nomagic.magicdraw.properties.Property
import com.nomagic.magicdraw.properties.PropertyResourceProvider
import com.nomagic.magicdraw.properties.StringProperty
import com.nomagic.ui.SwingImageIcon

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.options.resources.DynamicScriptsResources

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsOptions extends AbstractPropertyOptionsGroup( DynamicScriptsOptions.ID ) {

  override def getName(): String = DynamicScriptsResources.getString( DynamicScriptsOptions.NAME )

  override def setDefaultValues(): Unit = {
    val p = new DynamicScriptsConfigurationProperty( DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_ID, null, true )
    p.setResourceProvider( DynamicScriptsOptions.PROPERTY_RESOURCE_PROVIDER )
    addProperty( p )
  }

  def getDynamicScriptConfigurationFiles(): List[String] =
    getProperty( DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_ID ) match {
      case null              => List()
      case p: StringProperty => DynamicScriptsConfigurationProperty.getDynamicScriptConfigurationFiles( p )
      case _                 => List()
    }

  override def getIcon(): SwingImageIcon = super.getIcon()

}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsOptions {

  val ID = "DynamicScripts.Options"
  val NAME = "DYNAMIC_SCRIPT_OPTIONS_NAME"
  val DYNAMIC_SCRIPT_CONFIGURATION_FILES_ID = "DYNAMIC_SCRIPT_CONFIGURATION_FILES_ID"
  val DYNAMIC_SCRIPT_CONFIGURATION_FILES_GROUP = "DYNAMIC_SCRIPT_CONFIGURATION_FILES_GROUP"
  val DYNAMIC_SCRIPT_CONFIGURATION_FILES_NAME = "DYNAMIC_SCRIPT_CONFIGURATION_FILES_NAME"
  val DYNAMIC_SCRIPT_CONFIGURATION_FILES_DESC = "DYNAMIC_SCRIPT_CONFIGURATION_FILES_DESCRIPTION"

  def configureEnvironmentOptions(): DynamicScriptsOptions = {
    val envOptions = Application.getInstance().getEnvironmentOptions()
    val options = new DynamicScriptsOptions()
    envOptions.addGroup( options )
    envOptions.addEnvironmentChangeListener( DynamicScriptsPlugin.getInstance() )
    options
  }

  val PROPERTY_RESOURCE_PROVIDER = new PropertyResourceProvider() {

    override def getString( key: String, property: Property ): String = DynamicScriptsResources.getString( key )

  }
}
