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

package gov.nasa.jpl.dynamicScripts.magicdraw.options.resources

import com.nomagic.magicdraw.resources.ResourceManager
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

import scala.StringContext
import scala.Predef.{classOf,identity,String}
import scala.util.control.Exception._

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsResources {

  val BUNDLE_NAME = "gov.nasa.jpl.dynamicScripts.magicdraw.options.resources.DynamicScriptsResources"
  
  def getString(key: String): String =
    catching(classOf[java.util.MissingResourceException])
    .either( ResourceManager.getStringFor(key, BUNDLE_NAME, DynamicScriptsResources.getClass.getClassLoader) )
    .fold(
      (t: java.lang.Throwable) => {
        MDUML.getMDPluginsLog.error(s"DynamicScriptsResources.getString(key='$key') failed: ${t.getMessage}", t)
        ""
      },
      identity)

}