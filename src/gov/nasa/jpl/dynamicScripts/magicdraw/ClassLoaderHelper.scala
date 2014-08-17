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
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.TraversableOnce.OnceCanBuildFrom
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.nomagic.magicdraw.automaton.AutomatonPlugin
import com.nomagic.magicdraw.core.ApplicationEnvironment
import com.nomagic.magicdraw.plugins.PluginUtils
import com.nomagic.magicdraw.utils.MDLog
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.JName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.PluginContext
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ProjectContext
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicScriptInfo
import java.lang.reflect.Method

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object ClassLoaderHelper {

  def makeErrorMessageFor_createDynamicScriptClassLoader_Failure( action: DynamicScriptInfo, t: Throwable ): String =
     s"\nScript Context: '${action.context}'\nError: ${t.getClass().getName()}\nMessage: ${t.getMessage()}\n(do not submit!)"
     
  def makeErrorMessageFor_loadClass_null( action: DynamicScriptInfo ): String =
    s"\nScript class: '${action.className.jname}' not found\n(do not submit!)"
  
  def makeErrorMessageFor_lookupMethod_Failure( action: DynamicScriptInfo, t: Throwable ): String =
    s"\nScript class: '${action.className.jname}' not found\n(do not submit!)"
  
  def makeErrorMessageFor_invoke_Failure( t: Throwable ): String =
    s"\nScript execution failed: ${t.getClass().getName()}\nMessage: ${t.getMessage()}"
   
  def makeErrorMessageFor_InvocationTargetException( t: Throwable ): String =
    s"\nScript execution failed: ${t.getClass().getName()}\nMessage: ${t.getMessage()}\n(do not submit!)"
  
  def makeErrorMessageFor_invoke_Exception( t: Throwable ): String =
    s"\nScript execution failed: ${t.getClass().getName()}\nMessage: ${t.getMessage()}\n(do not submit!)"
  

  def lookupMethod( clazz: java.lang.Class[_], action: DynamicActionScript, arguments: java.lang.Class[_]* ): Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname, arguments: _*) match {
        case m: Method => Success( m )
        case null      => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
      }
    }
    catch {
      case ex: NoSuchMethodException => Failure( new IllegalArgumentException( s"method '${action.methodName.sname}()' not found in ${action.className.jname}" ) )
    }

  sealed abstract class ClassLoaderError( error: String ) extends RuntimeException( error ) {}

  sealed abstract class ClassLoaderException( error: String, t: Throwable ) extends RuntimeException( error, t ) {}

  case class DynamicScriptsProjectNotFound( projectName: String, projectPath: File )
    extends ClassLoaderError( s"DynamicScripts project '${projectName}' not found (expected: ${projectPath})" ) {}

  case class DynamicScriptsProjectIncomplete( projectName: String )
    extends ClassLoaderError( s"DynamicScripts project '${projectName}' does not resolve to any loadable URLs" ) {}

  case class DynamicScriptsPluginNotFound( pluginName: String )
    extends ClassLoaderError( s"DynamicScripts plugin '${pluginName}' not found" ) {}

  case class DynamicScriptsPluginNotEnabled( pluginName: String )
    extends ClassLoaderError( s"DynamicScripts plugin '${pluginName}' not enabled" ) {}

  case class DynamicScriptsPluginNotLoaded( pluginName: String )
    extends ClassLoaderError( s"DynamicScripts plugin '${pluginName}' not loaded" ) {}

  private var dynamicScriptsRootPath: String = null

  def getDynamicScriptsRootPath(): String = {
    if ( dynamicScriptsRootPath == null ) {
      dynamicScriptsRootPath = ApplicationEnvironment.getInstallRoot() + File.separator + "dynamicScripts"
    }
    dynamicScriptsRootPath
  }

  val jarFilenameFilter = new FilenameFilter() {
    override def accept( file: File, name: String ): Boolean = name.toLowerCase().endsWith( ".jar" )
  }

  def isFolderAvailable( f: File ): Boolean =
    if ( !f.exists() )
      false
    else if ( !f.canRead() )
      false
    else if ( !f.canExecute() )
      false
    else
      true

  def isDynamicActionScriptAvailable( das: DynamicActionScript ): Boolean = das.context match {
    case c: ProjectContext => isDynamicActionScriptAvailable( das, c )
    case c: PluginContext  => isDynamicActionScriptAvailable( das, c )
  }

  def isDynamicActionScriptAvailable( das: DynamicActionScript, c: PluginContext ): Boolean =
    PluginUtils.getPlugins().toList find { p => p.getDescriptor().getID() == c.pluginID.hname } match {
      case None      => false
      case Some( p ) => p.getDescriptor().isEnabled() && p.getDescriptor().isLoaded()
    }

  def isDynamicActionScriptAvailable( das: DynamicActionScript, c: ProjectContext ): Boolean = {
    val scriptProjectPath = getDynamicScriptsRootPath() + File.separator + c.project.jname + File.separator
    val scriptProjectDir = new File( scriptProjectPath )

    if ( !isFolderAvailable( scriptProjectDir ) )
      return false

    val scriptProjectBin = new File( scriptProjectPath + "bin" )
    if ( isFolderAvailable( scriptProjectBin ) )
      return true

    val scriptProjectLib = new File( scriptProjectPath + "lib" )
    val jars = if ( isFolderAvailable( scriptProjectLib ) ) scriptProjectLib.listFiles( jarFilenameFilter ) else Array[File]()
    return jars.nonEmpty
  }

  def resolveProjectPaths( urls: Try[List[URL]], projectName: JName ): Try[List[URL]] =

    urls match {
      case Failure( t ) => Failure( t )
      case Success( list ) =>

        val scriptProjectPath = getDynamicScriptsRootPath() + File.separator + projectName.jname + File.separator
        val scriptProjectDir = new File( scriptProjectPath )
        if ( !isFolderAvailable( scriptProjectDir ) )
          return Failure( DynamicScriptsProjectNotFound( projectName.jname, scriptProjectDir ) )

        val scriptProjectBin = new File( scriptProjectPath + "bin" )
        val binURL = if ( isFolderAvailable( scriptProjectBin ) ) Some( scriptProjectBin.toURI().toURL() ) else None

        val scriptProjectLib = new File( scriptProjectPath + "lib" )
        val jars =
          if ( isFolderAvailable( scriptProjectLib ) )
            scriptProjectLib.listFiles( jarFilenameFilter ).toList map ( _.toURI().toURL() ) toList
          else
            List[URL]()

        ( binURL, jars ) match {
          case ( None, Nil )         => Failure( DynamicScriptsProjectNotFound( projectName.jname, scriptProjectDir ) )
          case ( None, libs )        => Success( libs )
          case ( Some( url ), libs ) => Success( url :: libs )
        }
    }

  def createDynamicScriptClassLoader( s: DynamicActionScript, pluginContext: PluginContext ): Try[URLClassLoader] =
    PluginUtils.getPlugins().toList find { p => p.getDescriptor().getID() == pluginContext.pluginID.hname } match {
      case None => Failure( DynamicScriptsPluginNotFound( pluginContext.pluginID.hname ) )
      case Some( p ) =>
        p.getDescriptor() match {
          case null                                      => Failure( DynamicScriptsPluginNotFound( pluginContext.pluginID.hname ) )
          case pd if ( !pd.isEnabled() )                 => Failure( DynamicScriptsPluginNotEnabled( pluginContext.pluginID.hname ) )
          case pd if ( !pd.isLoaded() )                  => Failure( DynamicScriptsPluginNotLoaded( pluginContext.pluginID.hname ) )
          case pd if ( pd.isEnabled() && pd.isLoaded() ) => Success( new URLClassLoader( Array[URL](), p.getClass().getClassLoader() ) )
        }
    }

  def createDynamicScriptClassLoader( s: DynamicActionScript, projectContext: ProjectContext ): Try[URLClassLoader] = {
    val log = MDLog.getPluginsLog()
    val init: Try[List[URL]] = Success( Nil )
    val last: Try[List[URL]] = ( init /: ( projectContext.project +: projectContext.dependencies ) ) {
      ( urls: Try[List[URL]], project: JName ) => resolveProjectPaths( urls, project )
    }
    
    val init2last: Try[List[URL]] = ( init /: ( projectContext.project +: projectContext.dependencies ) ) ( resolveProjectPaths ( _, _ ) )
    
    last match {
      case Failure( t )   => Failure( t )
      case Success( Nil ) => Failure( DynamicScriptsProjectIncomplete( projectContext.prettyPrint( " " ) ) )
      case Success( urls ) =>
        log.info( s"gov.nasa.jpl.dynamicScripts.magicdraw - ${urls.size} URLs for ClassLoader for: ${s}${urls.mkString( "\n => ", "\n => ", "" )}" )
        Success( new URLClassLoader( urls.toArray, classOf[AutomatonPlugin].getClassLoader() ) )
    }
  }

  def createDynamicScriptClassLoader( s: DynamicActionScript ): Try[URLClassLoader] = s.context match {
    case c: PluginContext  => createDynamicScriptClassLoader( s, c )
    case c: ProjectContext => createDynamicScriptClassLoader( s, c )
  }

}