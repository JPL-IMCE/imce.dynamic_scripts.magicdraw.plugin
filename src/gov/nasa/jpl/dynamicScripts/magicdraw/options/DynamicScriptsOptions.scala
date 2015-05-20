/*
 * Replace this with your license text!
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
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

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
    MDUML.getPropertyOfOptionsGroup( this, DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_ID ) match {
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