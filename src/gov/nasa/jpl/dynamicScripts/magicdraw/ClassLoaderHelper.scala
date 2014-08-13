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
package gov.nasa.jpl.dynamicScripts.magicdraw

import java.io.File
import java.io.FilenameFilter
import java.net.URL
import java.net.URLClassLoader
import scala.collection.TraversableOnce.OnceCanBuildFrom
import scala.collection.JavaConversions._
import scala.language.implicitConversions
import scala.language.postfixOps
import com.nomagic.magicdraw.automaton.AutomatonPlugin
import com.nomagic.magicdraw.core.ApplicationEnvironment
import com.nomagic.magicdraw.utils.MDLog
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import com.nomagic.magicdraw.plugins.PluginUtils
import com.nomagic.magicdraw.plugins.Plugin

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object ClassLoaderHelper {

  private var imceScriptsRootPath: String = null
  
  def getIMCEScriptsRootPath(): String = {
    if (imceScriptsRootPath == null) {
      imceScriptsRootPath = ApplicationEnvironment.getInstallRoot() + File.separator + "imce.scripts"
    }
    imceScriptsRootPath
  } 
  
  val jarFilenameFilter = new FilenameFilter() {
    override def accept(file: File, name: String): Boolean = name.toLowerCase().endsWith(".jar")
  }
  
  def isFolderAvailable(f: File): Boolean = 
    if (!f.exists())
      false
    else if (!f.canRead())
      false
    else if (!f.canExecute())
      false
    else
      true
    
  def isDynamicActionScriptAvailable(das: DynamicActionScript): Boolean = das.context match {
      case c: ProjectContext => isDynamicActionScriptAvailable( das, c)
      case c: PluginContext => isDynamicActionScriptAvailable( das, c)
    }

  def isDynamicActionScriptAvailable(das: DynamicActionScript, c: PluginContext): Boolean =
    PluginUtils.getPlugins().toList find { p => p.getDescriptor().getID() == c.pluginID.hname } match { 
      case None => false
      case Some(p) => p.getDescriptor().isEnabled() && p.getDescriptor().isLoaded()
  }
    
  def isDynamicActionScriptAvailable(das: DynamicActionScript, c: ProjectContext): Boolean = {
    val scriptProjectPath = getIMCEScriptsRootPath() + File.separator + c.project.jname + File.separator
    val scriptProjectDir = new File(scriptProjectPath)
    
    if (!isFolderAvailable(scriptProjectDir))
      return false
      
    val scriptProjectBin = new File(scriptProjectPath + "bin")
    if (isFolderAvailable(scriptProjectBin)) 
      return true
    
		val scriptProjectLib = new File(scriptProjectPath + "lib")
    val jars = if (isFolderAvailable(scriptProjectLib)) scriptProjectLib.listFiles(jarFilenameFilter) else Array[File]()
    return jars.nonEmpty
  }
  
  def resolveProjectPaths(scriptProjectName: String, urls: List[URL]): List[URL] = {
		
    val scriptProjectPath = getIMCEScriptsRootPath() + File.separator + scriptProjectName + File.separator
    val scriptProjectDir = new File(scriptProjectPath)
    if (!isFolderAvailable(scriptProjectDir)) return Nil
     
    val scriptProjectBin = new File(scriptProjectPath + "bin")
    val binURL = if (isFolderAvailable(scriptProjectBin)) Some(scriptProjectBin.toURI().toURL()) else None
    
		val scriptProjectLib = new File(scriptProjectPath + "lib")
    val jars = 
      if (isFolderAvailable(scriptProjectLib)) 
        scriptProjectLib.listFiles(jarFilenameFilter).toList map (_.toURI().toURL()) toList 
      else 
        List[URL]()
		
		binURL.toList ++ jars
	}
  
  def createDynamicScriptClassLoader(s: DynamicActionScript, pluginContext: PluginContext): URLClassLoader = {
    val mdPlugin = PluginUtils.getPlugins().toList find { p => p.getDescriptor().getID() == pluginContext.pluginID.hname }
    require(mdPlugin.isDefined)
    new URLClassLoader(Array[URL](), mdPlugin.get.getClass().getClassLoader())
  }

  def createDynamicScriptClassLoader(s: DynamicActionScript, projectContext: ProjectContext): URLClassLoader = {
    val log = MDLog.getPluginsLog()
    val urls = (List[URL]() /: (projectContext.project +: projectContext.dependencies)) { (urls: List[URL], project: JName) => resolveProjectPaths(project.jname, urls) } toArray;
    log.info(s"gov.nasa.jpl.dynamicScripts.magicdraw - ${urls.size} URLs for ClassLoader for: ${s}${urls.mkString("\n => ", "\n => ", "")}")
    new URLClassLoader(urls, classOf[AutomatonPlugin].getClassLoader())
  }
  
  def createDynamicScriptClassLoader(s: DynamicActionScript): URLClassLoader = s.context match {
    case c: PluginContext => createDynamicScriptClassLoader(s, c)
    case c: ProjectContext => createDynamicScriptClassLoader(s, c)
  }
 
}