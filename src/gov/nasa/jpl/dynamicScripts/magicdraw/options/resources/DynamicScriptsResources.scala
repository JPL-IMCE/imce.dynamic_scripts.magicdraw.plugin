/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.options.resources

import com.nomagic.magicdraw.resources.ResourceManager

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsResources {

  val BUNDLE_NAME = "gov.nasa.jpl.dynamicScripts.magicdraw.options.resources.DynamicScriptsResources"
  
  def getString(key: String): String = 
    ResourceManager.getStringFor(key, BUNDLE_NAME, DynamicScriptsResources.getClass().getClassLoader())
}