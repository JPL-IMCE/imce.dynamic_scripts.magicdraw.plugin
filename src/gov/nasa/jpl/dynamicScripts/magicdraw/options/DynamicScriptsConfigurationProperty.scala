/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.options

import com.nomagic.magicdraw.properties.StringProperty

import gov.nasa.jpl.dynamicScripts.magicdraw.options.resources.DynamicScriptsResources

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsConfigurationProperty(id: String, value: Object, multiline: Boolean) extends StringProperty(id, value, multiline) {

  override def getValueStringRepresentation(): String = getString()

  def getDynamicScriptConfigurationFiles(): List[String] =
    getString() match {
      case null          => List()
      case ""            => List()
      case files: String => files.split( "\n" ).toList
    }
  
  override def getName(): String =
    DynamicScriptsResources.getString(DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_NAME)
    
  override def getGroup(): String =
    DynamicScriptsResources.getString(DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_GROUP)
    
  override def getDescriptionID(): String =
    DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_DESC
    
  override def getDescription(): String =
    DynamicScriptsResources.getString(DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_DESC)

}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsConfigurationProperty {
  
   def getDynamicScriptConfigurationFiles(p: StringProperty): List[String] =
    p.getString() match {
      case null          => List()
      case ""            => List()
      case files: String => files.split( "\n" ).toList
    }
}