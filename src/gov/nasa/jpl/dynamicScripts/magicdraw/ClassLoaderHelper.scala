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
package gov.nasa.jpl.dynamicScripts.magicdraw

import java.awt.event.ActionEvent
import java.io.File
import java.io.FilenameFilter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.{Class, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, Object}
import java.lang.{NoClassDefFoundError, NoSuchMethodException, RuntimeException}
import java.lang.{SecurityException, System, Throwable}
import java.net.URL
import java.net.URLClassLoader

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.openapi.uml.SessionManager
import com.nomagic.magicdraw.plugins.Plugin
import com.nomagic.magicdraw.plugins.PluginUtils

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicScriptInfo
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.HName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.JName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.PluginContext
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ProjectContext
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.{TraversePath, MDGUILogHelper, MDUML}

import java.nio.file.Paths
import java.nio.file.Files

import java.nio.charset.spi.CharsetProvider
import java.nio.charset.StandardCharsets

import gov.nasa.jpl.dynamicScripts.magicdraw.validation.MagicDrawValidationDataResultsException

import scala.collection.JavaConversions._
import scala.collection.immutable._
import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.{Any, Array, Boolean, Long, None, Option, Some, StringContext, Unit}
import scala.util.control.Exception._
import scala.util.{Failure, Success, Try}
import scala.Predef.{augmentString, classOf, refArrayOps, require, String}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object ClassLoaderHelper {

  def reportError
  ( s: DynamicScriptInfo, message: String, t: Throwable )
  : Unit = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog()

    log.error( message + t.getMessage, t )
    guiLog.showError( message + t.getMessage, t )
  }

  def makeErrorMessageFor_createDynamicScriptClassLoader_Failure
  ( action: DynamicScriptInfo, t: Throwable )
  : String =
    s"\nCannot load script: ${t.getClass.getName}\n${t.getMessage}\n(do not submit!)"

  def makeErrorMessageFor_loadClass_error
  ( action: DynamicScriptInfo, t: Throwable )
  : String =
    s"\nLookup for script class: '${action.className.jname}' failed\nException: "+
    s"${t.getClass.getName}\nMessage: ${t.getMessage}\n(do not submit!)"

  def makeErrorMessageFor_loadClass_null
  ( action: DynamicScriptInfo ): String =
    s"\nScript class: '${action.className.jname}' not found\n(do not submit!)"

  def makeErrorMessageFor_lookupMethod_null
  ( clazz: java.lang.Class[_], action: DynamicScriptInfo, argumentTypes: java.lang.Class[_]* )
  : String =
    s"""|Script method: '${action.methodName.sname}' not found
        |argument types:${( argumentTypes map { t => t.getName } ) mkString ( "\n  ", "\n  ", "" )}
        |(do not submit!)""".stripMargin

  def makeErrorMessageFor_lookupMethod_error
  ( t: Throwable, clazz: java.lang.Class[_], action: DynamicScriptInfo, argumentTypes: java.lang.Class[_]* )
  : String =
    s"""|Script method: '${action.methodName.sname}' failed
        |Argument types:${( argumentTypes map { t => t.getName } ) mkString ( "\n  ", "\n  ", "" )}
        |Exception: ${t.getClass.getName}
        |Exception message: ${t.getMessage}
        |(do not submit!)""".stripMargin

  def makeErrorMessageFor_invoke_Failure
  ( t: Throwable )
  : String =
    s"\nScript execution failed: ${t.getClass.getName}\nMessage: ${t.getMessage}"

  def makeErrorMessageFor_InvocationTargetException
  ( t: Throwable )
  : String =
    s"\nScript execution failed: ${t.getClass.getName}\nMessage: ${t.getMessage}\n(do not submit!)"

  def makeErrorMessageFor_invoke_Exception
  ( t: Throwable )
  : String =
    s"\nScript execution failed: ${t.getClass.getName}\nMessage: ${t.getMessage}\n(do not submit!)"

  case class ResolvedClassAndMethod
  ( s: DynamicScriptInfo, c: Class[_ <: Any], m: Method ) {}

  sealed abstract class ClassLoaderError( error: String )
    extends RuntimeException( error ) {}

  sealed abstract class ClassLoaderException( error: String, t: Throwable )
    extends RuntimeException( error, t ) {}

  case class DynamicScriptsProjectNotFound( projectName: String, projectPath: File )
    extends ClassLoaderError( s"DynamicScripts project '$projectName' not found (expected: $projectPath)" ) {}

  case class DynamicScriptsProjectIncomplete( projectName: String )
    extends ClassLoaderError( s"DynamicScripts project '$projectName' does not resolve to any loadable URLs" ) {}

  case class DynamicScriptsPluginNotFound( pluginName: String )
    extends ClassLoaderError( s"DynamicScripts plugin '$pluginName' not found" ) {}

  case class DynamicScriptsPluginNotEnabled( pluginName: String )
    extends ClassLoaderError( s"DynamicScripts plugin '$pluginName' not enabled" ) {}

  case class DynamicScriptsPluginNotLoaded( pluginName: String )
    extends ClassLoaderError( s"DynamicScripts plugin '$pluginName' not loaded" ) {}

  case class DynamicScriptsClassNotFound( message: String )
    extends ClassLoaderError( message ) {}

  case class DynamicScriptsMethodNotFound( message: String )
    extends ClassLoaderError( message ) {}

  case class DynamicScriptsClassLookupError( s: DynamicScriptInfo, t: Throwable )
    extends ClassLoaderException( makeErrorMessageFor_loadClass_error( s, t ), t ) {}

  def lookupMethod
  ( clazz: java.lang.Class[_], action: DynamicScriptInfo, argumentTypes: java.lang.Class[_]* )
  : Try[Method] =
    try {
      clazz.getMethod( action.methodName.sname, argumentTypes: _* ) match {
        case m: Method =>
          Success( m )
        case null      =>
          Failure( DynamicScriptsMethodNotFound(
            makeErrorMessageFor_lookupMethod_null( clazz, action, argumentTypes: _* ) ) )
      }
    }
    catch {
      case ex: NoSuchMethodException =>
        Failure(
          DynamicScriptsMethodNotFound(
            makeErrorMessageFor_lookupMethod_error( ex, clazz, action, argumentTypes: _* ) ) )
    }

  def lookupClassAndMethod
  ( scriptCL: URLClassLoader, s: DynamicScriptInfo, argumentTypes: java.lang.Class[_]* )
  : Try[ResolvedClassAndMethod] = {
    val c: Class[_] = try {
      scriptCL.loadClass( s.className.jname ) match {
        case null        =>
          return Failure( DynamicScriptsClassNotFound( makeErrorMessageFor_loadClass_null( s ) ) )
        case c: Class[_] =>
          c
      }
    }
    catch {
      case ex @ ( _: ClassNotFoundException | _: SecurityException | _: NoSuchMethodException |
                  _: IllegalAccessException | _: NoClassDefFoundError ) =>
        return Failure( DynamicScriptsClassLookupError( s, ex ) )
    }

    val m: Method = lookupMethod( c, s, argumentTypes: _* ) match {
      case Failure( t ) =>
        return Failure( t )
      case Success( m ) =>
        m
    }

    Success( ResolvedClassAndMethod( s, c, m ) )
  }

  /**
   * Wrapper for a Throwable indicating that it's been already reported
   */
  case class ReportedException( t: Throwable ) extends Throwable

  /**
   * Reports Failure results of 'invoke()' unless they've been already reported
   */
  def invokeAndReport
  ( previousTime: Long, p: Project, ev: ActionEvent, cm: ResolvedClassAndMethod, argumentValues: Object* )
  : Try[Any] =
    invoke( previousTime, p, ev, cm, argumentValues: _* ) match {
      case Success( x ) =>
        Success( x )
      case Failure( t @ ( _: MagicDrawValidationDataResultsException | _: ReportedException ) ) =>
        Failure( t )
      case Failure( t ) =>
        ClassLoaderHelper.reportError( cm.s, t.getMessage, t )
        Failure( t )
    }

  def ignoreResultOrthrowFailure[T]
  (x: Try[T])
  : Unit =
  x match {
    case Failure(t) =>
      throw t
    case _ =>
      ()
  }

  /**
    * Safely invoke a DynamicScriptInfo method
    *
    * @param previousTime whose difference with the time after invoking the method will be logged
    * @param p the active MagicDraw project
    * @param ev the event that triggered this invocation
    * @param cm the resolved JVM Class and Method from a DynamicScriptInfo
    * @param argumentValues will be passed to the method invocation
    * @return the result of invoking the resolved Method with the project, the event,
    *         the DynamicScriptInfo and the argument values
    *
    * The result is checked for one of the following:
    * - 'Failure( t: MagicDrawValidationDataResultsException )' =>
    *   'Failure( t )', after opening MagicDraw's validation window
    *
    * - 'Failure( t )' =>
    *   'Failure( t )', possibly due to invocation-related runtime exceptions
    * - 'Success( None )' =>
    *   'Success( Unit )'
    * - 'Success( Some( MagicDrawValidationDataResults ) )' =>
    *   opens MagicDraw's validation window and returns the 'Try[Unit]' result of executing post-processing actions
    *  - 'Success( Some( any ) )' =>
    *    'Success( any )'
    * - 'Success( any )' =>
    *   'Success( any )'
    * - 'any' =>
    *   'Success( any )'
    */
  def invoke
  ( previousTime: Long, p: Project, ev: ActionEvent, cm: ResolvedClassAndMethod, argumentValues: Object* )
  : Try[Any] = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog()

    val message = cm.s.prettyPrint( "" ) + "\n"

    val sm = SessionManager.getInstance()
    if ( p != null )
      sm.createSession( p, message )

    def closeSessionIfNeeded(): Unit =
      if ( p != null && sm.isSessionCreated( p ) ) {
        sm.closeSession( p )
      }

    def cancelSessionIfNeeded(): Unit =
      if ( p != null && sm.isSessionCreated( p ) ) {
        sm.cancelSession( p )
      }

    val actionAndArgumentValues = Seq( p, ev, cm.s ) ++ argumentValues toSeq;
    try {

      import validation.internal.MDValidationAPIHelper._

      val r = cm.m.invoke( null, actionAndArgumentValues: _* )

      val currentTime = System.currentTimeMillis()
      log.info( s"$message took ${currentTime - previousTime} ms" )

      r match {
        case Failure( t @ MagicDrawValidationDataResultsException( r ) ) =>
          p.showMDValidationDataResultsAndExecutePostSessionActions( sm, r, message )
          Failure( t )

        case Failure( t ) =>
          cancelSessionIfNeeded()
          Failure( t )

        case Success( None ) =>
          closeSessionIfNeeded()
          Success( () )

        case Success( s ) => s match {
          case Some( r: validation.MagicDrawValidationDataResults ) =>
            p.showMDValidationDataResultsAndExecutePostSessionActions( sm, r, message )

          case Some( any ) =>
            closeSessionIfNeeded()
            Success( any )

          case None =>
            closeSessionIfNeeded()
            Success( () )

          case any =>
            closeSessionIfNeeded()
            Success( any )
        }

        case any =>
          closeSessionIfNeeded()
          Success( any )
      }
    }
    catch {
      case ex: InvocationTargetException =>
        cancelSessionIfNeeded()
        val t = ex.getTargetException match { case null => ex; case t => t }
        val ex_message = message + s"\nError: ${t.getClass.getName}\nMessage: ${t.getMessage}\n(do not submit!)"
        ClassLoaderHelper.reportError( cm.s, ex_message, t )
        Failure( ReportedException( t ) )

      case t: IllegalArgumentException =>
        cancelSessionIfNeeded()
        val parameterTypes =
          cm.m.getParameterTypes
          .map ( _.getName )
          .mkString ( "\n parameter type: ", "\n parameter type: ", "" )
        val parameterValues =
          actionAndArgumentValues
          .map ( getArgumentValueTypeName )
          .mkString ( "\n argument type: ", "\n argument type: ", "" )
        val ex_message =
          message +
            s"\nError: ${t.getClass.getName}\n"+
            s"Message: ${t.getMessage}\n$parameterTypes\n$parameterValues(do not submit!)"
        ClassLoaderHelper.reportError( cm.s, ex_message, t )
        Failure( ReportedException( t ) )

      case t @ ( _: ClassNotFoundException | _: SecurityException |
                 _: NoSuchMethodException | _: IllegalAccessException ) =>
        cancelSessionIfNeeded()
        val ex_message = message + s"\nError: ${t.getClass.getName}\nMessage: ${t.getMessage}\n(do not submit!)"
        ClassLoaderHelper.reportError( cm.s, ex_message, t )
        Failure( ReportedException( t ) )

    }
    finally {
      cancelSessionIfNeeded()
    }
  }

  def getArgumentValueTypeName( value: Any ): String =
    if ( null == value ) "<null>"
    else value.getClass.getName

  private var dynamicScriptsRootPath: String = null

  def getDynamicScriptsRootPath: String = {
    if ( dynamicScriptsRootPath == null ) {
      dynamicScriptsRootPath = MDUML.getInstallRoot + File.separator + "dynamicScripts"
    }
    dynamicScriptsRootPath
  }

  val jarFilenameFilter = new FilenameFilter() {
    override def accept( file: File, name: String ): Boolean = name.toLowerCase.endsWith( ".jar" )
  }

  def isFolderAvailable( f: File ): Boolean =
    if ( !f.exists() )
      false
    else if ( !f.canRead )
      false
    else if ( !f.canExecute )
      false
    else
      true

  def isDynamicActionScriptAvailable( ds: DynamicScriptInfo ): Boolean = ds.context match {
    case c: ProjectContext => isDynamicActionScriptAvailable( ds, c )
    case c: PluginContext  => isDynamicActionScriptAvailable( ds, c )
  }

  def getPluginIfLoadedAndEnabled( pluginID: String ): Option[Plugin] =
    PluginUtils.getPlugins.toList find { p => p.getDescriptor.getID == pluginID } match {
      case None => None
      case Some( p ) =>
        if ( p.getDescriptor.isEnabled && p.getDescriptor.isLoaded ) Some( p )
        else None
    }

  def isDynamicActionScriptAvailable( ds: DynamicScriptInfo, c: PluginContext ): Boolean =
    getPluginIfLoadedAndEnabled( c.pluginID.hname ).isDefined

  def isDynamicActionScriptAvailable( ds: DynamicScriptInfo, c: ProjectContext ): Boolean = {
    val scriptProjectPath = getDynamicScriptsRootPath + File.separator + c.project.jname + File.separator
    val scriptProjectDir = new File( scriptProjectPath )

    c.requiresPlugin match {
      case None => ()
      case Some( rp: HName ) =>
        if ( getPluginIfLoadedAndEnabled( rp.hname ).isEmpty )
          return false
    }

    if ( !isFolderAvailable( scriptProjectDir ) )
      return false

    val scriptProjectBin = new File( scriptProjectPath + "bin" )
    if ( isFolderAvailable( scriptProjectBin ) )
      return true

    val scriptProjectLib = new File( scriptProjectPath + "lib" )
    val jars = if ( isFolderAvailable( scriptProjectLib ) )
      TraversePath.listFilesRecursively(scriptProjectLib, jarFilenameFilter)
    else
      List()
    jars.nonEmpty
  }

  val classpathLibEntry = """(?m)<classpathentry.*?\s+?kind="lib".*?\s+?path="(.*?)".*?/>""".r
  val classpathConEntry = """(?m)<classpathentry.*?\s+?kind="con".*?\s+?path="gov\.nasa\.jpl\.magicdraw\.CLASSPATH_LIB_CONTAINER/(.*?)".*?/>""".r
  val classpathSrcEntry = """(?m)<classpathentry.*?\s+?kind="src".*?\s+?path="/(.*?)".*?/>""".r
  val classpathBinEntry = """(?m)<classpathentry.*?\s+?kind="output".*?\s+?path="(.*?)".*?/>""".r

  def resolveProjectPaths( urls: Try[List[URL]], projectName: JName ): Try[List[URL]] =

    urls match {
      case Failure( t ) => Failure( t )
      case Success( list ) =>

        val root = MDUML.getApplicationInstallDir

        val scriptProjectPathname = getDynamicScriptsRootPath + File.separator + projectName.jname + File.separator
        val ( scriptProjectPath, scriptProjectDir ) = try {
          val path = Paths.get( new File( scriptProjectPathname ).toURI ).toRealPath()
          val dir = path.toFile
          ( path, dir )
        } catch {
          case t: Throwable =>
            return Failure( t )
        }

        if ( !isFolderAvailable( scriptProjectDir ) )
          return Failure( DynamicScriptsProjectNotFound( projectName.jname, scriptProjectDir ) )

        val classpathFilepath = scriptProjectPath.resolve( ".classpath" )
        val classpathURLs =
        if ( Files.isReadable( classpathFilepath ) && Files.isRegularFile( classpathFilepath ) )
          nonFatalCatch[Try[List[URL]]]
            .withApply { (t: java.lang.Throwable) => Failure(t) }
            .apply({
              val classpathContent = new String(Files.readAllBytes(classpathFilepath))
              val cons = classpathConEntry.findAllMatchIn(classpathContent).flatMap { m =>
                for {
                  relPath <- m.group(1).split(",")
                  absPath = root.toPath.resolve(relPath)
                  absDir = absPath.toFile
                  if absDir.exists && absDir.isDirectory
                } yield absDir.toURI.toURL
              } toList
              val libs = classpathLibEntry.findAllMatchIn(classpathContent).map { m =>
                val libpath = scriptProjectPath.resolve(m.group(1))
                require(Files.isRegularFile(libpath), s"lib path: $libpath")
                require(Files.isReadable(libpath), s"lib path: $libpath")
                libpath.toUri.toURL
              } toList
              val bins = classpathBinEntry.findAllMatchIn(classpathContent).flatMap { m =>
                val binpath = scriptProjectPath.resolve(m.group(1))
                if (Files.isDirectory(binpath) && Files.isReadable(binpath))
                  Some(binpath.toUri.toURL)
                else
                  None
              } toList
              val srcs = classpathSrcEntry.findAllMatchIn(classpathContent).map { m => JName(m.group(1)) }
              val resolved0: Try[List[URL]] = Success(bins ++ libs ++ cons)
              val resolvedN = (resolved0 /: srcs) (resolveProjectPaths)
              resolvedN
            })
        else
          Success(Nil)

        classpathURLs match {
          case Failure(t) =>
            Failure(t)
          case Success(resolvedURLs) =>
            val scriptProjectBin = scriptProjectPath.resolve("bin").toFile
            val binURL =
              if (!isFolderAvailable(scriptProjectBin))
                List()
              else
                List(scriptProjectBin.toURI.toURL)

            val scriptProjectLib = scriptProjectPath.resolve("lib").toFile
            val jars =
              if (!isFolderAvailable(scriptProjectLib))
                List()
              else
                TraversePath.listFilesRecursively(scriptProjectLib, jarFilenameFilter) map (_.toURI.toURL)

            val combinedList = (list /: (resolvedURLs ::: binURL ::: jars)) { case (us, u) =>
              if (us.contains(u)) us else us :+ u
            }

            if (combinedList.isEmpty)
              Failure(DynamicScriptsProjectNotFound(projectName.jname, scriptProjectDir))
            else
              Success(combinedList)
        }
    }

  def createDynamicScriptClassLoader( s: DynamicScriptInfo, pluginContext: PluginContext ): Try[URLClassLoader] =
    getPluginIfLoadedAndEnabled( pluginContext.pluginID.hname ) match {
      case None =>
        Failure( DynamicScriptsPluginNotFound( pluginContext.pluginID.hname ) )
      case Some( p ) =>
        p.getDescriptor match {
          case null                              =>
            Failure( DynamicScriptsPluginNotFound( pluginContext.pluginID.hname ) )
          case pd if !pd.isEnabled               =>
            Failure( DynamicScriptsPluginNotEnabled( pluginContext.pluginID.hname ) )
        case pd if !pd.isLoaded                  =>
            Failure( DynamicScriptsPluginNotLoaded( pluginContext.pluginID.hname ) )
          case pd if pd.isEnabled && pd.isLoaded =>
            Success( new URLClassLoader( Array[URL](), p.getClass.getClassLoader ) )
        }
    }

  def createDynamicScriptClassLoader
  ( s: DynamicScriptInfo, projectContext: ProjectContext )
  : Try[URLClassLoader] = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog()

    val init: Try[List[URL]] = Success( Nil )
    val projectPaths = projectContext.project +: projectContext.dependencies
    val last: Try[List[URL]] = ( init /: projectPaths )( resolveProjectPaths )

    val parentClassLoader = projectContext.requiresPlugin match {
      case None =>
        classOf[DynamicScriptsPlugin].getClassLoader
      case Some( rp ) =>
        getPluginIfLoadedAndEnabled( rp.hname ) match {
        case None              =>
          return Failure( DynamicScriptsPluginNotFound( rp.hname ) )
        case Some( p: Plugin ) =>
          p.getClass.getClassLoader
      }
    }

    last match {
      case Failure( t )   =>
        Failure( t )
      case Success( Nil ) =>
        Failure( DynamicScriptsProjectIncomplete( projectContext.prettyPrint( " " ) ) )
      case Success( urls ) =>
        log.info(
          s"gov.nasa.jpl.dynamicScripts.magicdraw - ${urls.size} URLs for ClassLoader for: "+
          s"$s${urls.mkString( "\n => ", "\n => ", "" )}" )
        Success( new URLClassLoader( urls.toArray, parentClassLoader ) )
    }
  }

  def createDynamicScriptClassLoader
  ( s: DynamicScriptInfo )
  : Try[URLClassLoader] = s.context match {
    case c: PluginContext  =>
      createDynamicScriptClassLoader( s, c )
    case c: ProjectContext =>
      createDynamicScriptClassLoader( s, c )
  }

}