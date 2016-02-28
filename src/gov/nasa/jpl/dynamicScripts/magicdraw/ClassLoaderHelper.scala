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
import java.lang.{Class, ClassLoader, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, Object}
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
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.{MDGUILogHelper, MDUML, TraversePath}
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper._
import java.nio.file.Paths
import java.nio.file.{Files, Path}
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
import scala.Predef.{String, augmentString, classOf, refArrayOps, require}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object ClassLoaderHelper {

  def reportError
  ( s: DynamicScriptInfo, message: String, t: Throwable )
  : Unit = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog

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

  case class DynamicScriptsProjectAccessException( projectName: JName, t: Throwable )
    extends ClassLoaderException(
      s"DynamicScripts project '$projectName' access exception: ${t.getMessage}", t ) {}

  case class DynamicScriptsProjectNotFound( projectName: JName, projectPath: File )
    extends ClassLoaderError(
      s"DynamicScripts project '$projectName' not found (expected: $projectPath)" ) {}

  case class DynamicScriptsProjectMissingClasspath( projectName: JName, classpathFile: Path )
    extends ClassLoaderError(
      s"DynamicScripts project '$projectName' is missing '.classpath' file (expected: $classpathFile)" ) {}

  case class DynamicScriptsProjectEmptyClasspath( projectName: JName, classpathFile: Path )
    extends ClassLoaderError(
      s"DynamicScripts project '$projectName' .classpath yields no jars!, check: $classpathFile" ) {}

  case class DynamicScriptsPluginNotFound( pluginName: String )
    extends ClassLoaderError(
      s"DynamicScripts plugin '$pluginName' not found" ) {}

  case class DynamicScriptsPluginNotEnabled( pluginName: String )
    extends ClassLoaderError(
      s"DynamicScripts plugin '$pluginName' not enabled" ) {}

  case class DynamicScriptsPluginNotLoaded( pluginName: String )
    extends ClassLoaderError(
      s"DynamicScripts plugin '$pluginName' not loaded" ) {}

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
    val log = guiLog.getMDPluginsLog

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

  private val unavailableDynamicScriptsExplanation =
    new scala.collection.mutable.HashMap[DynamicScriptInfo, Option[String]]()

  def clearUnavailableDynamicScriptsExplanationCache(): Unit = {
    unavailableDynamicScriptsExplanation.clear()
  }

  /**
    * Is a plugin or project defined dynamic script action available for invocation?
    *
    * @param ds A plugin or project defined DynamicScript
    * @return true if ds can be invoked
    */
  def isDynamicActionScriptAvailable( ds: DynamicScriptInfo ): Boolean = {
    val previousExplanation
    : Option[String]
    = unavailableDynamicScriptsExplanation.getOrElse(ds, Option.empty[String])

    val isAvailable =
      ds.context match {
        case c: ProjectContext =>
          isDynamicActionScriptAvailable(ds, c)
        case c: PluginContext =>
          isDynamicActionScriptAvailable(ds, c)
      }

    if (isAvailable)
      unavailableDynamicScriptsExplanation.update(ds, Some("OK"))

    val currentExplanation
    : Option[String]
    = unavailableDynamicScriptsExplanation.getOrElse(ds, Option.empty[String])
    require(currentExplanation.isDefined, "Check the logic of isDynamicScriptAvailable(ds, c)")

    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog

    (previousExplanation, currentExplanation) match {
      case (None, Some("OK")) =>
        ()
      case (None, Some(explanation)) =>
        // report the 1st explanation for the unavailability of the dynamic script ds
        val message =
          s"Unavailable DynamicScript report: $explanation\n"+ds.prettyPrint("  ")
        val lines = message.split("\n")
        lines.foreach { line => guiLog.log(line) }
        log.warn(message)
        ()
      case (Some(explanation1), Some("OK")) =>
        if (explanation1.compareTo("OK") != 0) {
          // report the new availability of the dynamic script ds
          val message =
            s"Available DynamicScript change:\n(previously unavailable because: $explanation1)\n"+ds.prettyPrint("  ")
          val lines = message.split("\n")
          lines.foreach { line => guiLog.log(line) }
          log.info(message)
        }
      case (Some(explanation1), Some(explanation2)) =>
        if (explanation1.compareTo(explanation2) != 0) {
          // report the updated explanation for the unavailability of the dynamic script ds
          val message =
            s"Unavailable DynamicScript update: $explanation2\n"+ds.prettyPrint("  ")
          val lines = message.split("\n")
          lines.foreach { line => guiLog.log(line) }
          log.warn(message)
        }
      case (_, None) =>
        // cannot happen.
    }

    isAvailable
  }

  def getPluginIfLoadedAndEnabled( pluginID: String ): Option[Plugin] =
    PluginUtils.getPlugins.toList find { p => p.getDescriptor.getID == pluginID } match {
      case None =>
        None
      case Some( p ) =>
        if ( p.getDescriptor.isEnabled && p.getDescriptor.isLoaded )
          Some( p )
        else
          None
    }

  /**
    * Is a plugin-defined dynamic script action available for invocation?
    *
    * @todo use a caching technique based on monitoring changes to the dynamic script's file
    * @param ds A Dynamic Script defined in a plugin
    * @param c The MagicDraw plugin context where ds is implemented
    * @return true if ds can be invoked
    */
  def isDynamicActionScriptAvailable( ds: DynamicScriptInfo, c: PluginContext ): Boolean = {
    val isAvailable = getPluginIfLoadedAndEnabled(c.pluginID.hname).isDefined
    if (!isAvailable)
      unavailableDynamicScriptsExplanation.update(
        ds,
        Some(s"Missing required plugin: ${c.pluginID.hname}"))
    isAvailable
  }

  /**
    * Is a project-defined dynamic script action available for invocation?
    *
    * @todo use a caching technique based on monitoring changes to the dynamic script's file
    *        and the project context '.classpath'
    *
    *        It is too expensive to resolve the project's classpath:
    *        {{{
    *          val init: Try[Some[URLPaths]] = resolveProjectPaths(Success(None), c.project)
    *          val last: Try[Some[URLPaths]] = ( init /: c.dependencies )( resolveProjectPaths )
    *          last match {
    *            case Failure( t ) =>
    *              unavailableDynamicScriptsExplanation.update(ds, Some(t.getMessage))
    *              false
    *            case Success( _ ) =>
    *              true
    *          }
    *        }}}
    * @param ds A Dynamic Script defined in a project
    * @param c The MagicDraw DynamicScript project context where ds is implemented
    * @return true if ds can be invoked
    */
  def isDynamicActionScriptAvailable( ds: DynamicScriptInfo, c: ProjectContext ): Boolean = {
    val scriptProjectPath = getDynamicScriptsRootPath + File.separator + c.project.jname + File.separator
    val scriptProjectDir = new File( scriptProjectPath )

    c.requiresPlugin match {
      case None =>
        true
      case Some( rp: HName ) =>
        if ( getPluginIfLoadedAndEnabled( rp.hname ).isEmpty ) {
          unavailableDynamicScriptsExplanation.update(
            ds,
            Some(s"Missing required plugin: ${rp.hname}"))
          false
        } else
          true
    }
  }

  val classpathLibEntry =
    """(?m)<classpathentry.*?\s+?kind="lib".*?\s+?path="(.*?)".*?/>""".r
  val classpathConEntry =
    """(?m)<classpathentry.*?\s+?kind="con".*?\s+?path="gov\.nasa\.jpl\.magicdraw\.CLASSPATH_LIB_CONTAINER/(.*?)".*?/>""".r
  val classpathSrcEntry =
    """(?m)<classpathentry.*?\s+?kind="src".*?\s+?path="/(.*?)".*?/>""".r
  val classpathBinEntry =
    """(?m)<classpathentry.*?\s+?kind="output".*?\s+?path="(.*?)".*?/>""".r

  /**
    * Wrapper for constructing a URL ClassLoader in terms of a set of
    * - 'bin' directories (*.class files) and
    * - 'jars' files
    *
    * @param bins A set of URLs for 'bin' directories containing '*.class' files
    * @param jars A set of URLs for '*.jar' files
    */
  case class URLPaths
  (bins: Set[URL],
   jars: Set[URL]) {

    require(bins.nonEmpty || jars.nonEmpty)

    def merge(other: Option[URLPaths]): URLPaths =
      other.fold[URLPaths](this) { that =>
        URLPaths(bins ++ that.bins, jars ++ that.jars)
      }

    def toURLClassLoader(parent: ClassLoader): URLClassLoader =
      new URLClassLoader(classpath, parent)

    lazy val classpath: Array[URL] =
      (bins.toList.sortBy(_.toString) ::: jars.toList.sortBy(_.toString)).toArray
  }

  /**
    * Resolve the classpath URls for a DynamicScript project
    *
    * @param urls Optionally, resolved URLs for other projects to be appended to this project's resolved URLs
    * @param projectName the name of a MagicDraw DynamicScripts project to resolve its classpath URLs
    * @return If the DynamicScript project has an Eclipse '.classpath' use it to determine the project's classpath:
    *         - recursively search for '*.jar' in all .classpath entries of the form:
    *           <classpathentry kind="lib" path="$$project_relative_or_absolute_dir"/>
    *         - recursively search for '*.jar' in all MD-relative paths in all .classpath entries of the form:
    *           <classpathentry kind="con" path="gov.nasa.jpl.magicdraw.CLASSPATH_LIB_CONTAINER/{,$$md_relative_path}*"/>
    *         - use as '*.class' files directories all entries of the form:
    *           <classpathentry kind="output" path="$$project_relative_dir"/>
    */
  def resolveProjectPaths( urls: Try[Option[URLPaths]], projectName: JName )
  : Try[Some[URLPaths]] =

    urls match {
      case Failure( t ) => Failure( t )
      case Success( urlPaths ) =>

        val root = MDUML.getApplicationInstallDir

        val scriptProjectPathname = getDynamicScriptsRootPath + File.separator + projectName.jname + File.separator
        val ( scriptProjectLogicalPath, scriptProjectRealPath, scriptProjectRealDir ) = try {
          val lpath = Paths.get( new File( scriptProjectPathname ).toURI )
          val rpath = lpath.normalize().toRealPath()
          val dir = rpath.toFile
          ( lpath, rpath, dir )
        } catch {
          case t: Throwable =>
            return  Failure( DynamicScriptsProjectAccessException( projectName, t ) )
        }

        if ( !isFolderAvailable( scriptProjectRealDir ) )
          return Failure( DynamicScriptsProjectNotFound( projectName, scriptProjectRealDir ) )

        val classpathFilepath = scriptProjectRealPath.resolve( ".classpath" )

        val classpathURLs
        : Try[Some[URLPaths]]
        = if ( Files.isReadable( classpathFilepath ) && Files.isRegularFile( classpathFilepath ) )
          nonFatalCatch[Try[Some[URLPaths]]]
            .withApply { (t: java.lang.Throwable) => Failure(t) }
            .apply({
              val classpathContent = new String(Files.readAllBytes(classpathFilepath))

              val cons = classpathConEntry.findAllMatchIn(classpathContent).flatMap { m =>
                for {
                  relPath <- m.group(1).split(",")
                  absPath = root.toPath.resolve(relPath).normalize().toRealPath()
                  absDir = absPath.toFile
                  if absDir.exists && absDir.isDirectory
                  jarFile <- TraversePath.listFilesRecursively(absDir, jarFilenameFilter)
                  jarURL = jarFile.toURI.toURL
                } yield jarURL
              } toSet

              val jars = classpathLibEntry.findAllMatchIn(classpathContent).map { m =>
                val entry = m.group(1)
                if (entry.startsWith(File.separator)) {
                  val jarpath = new File(entry).toPath.normalize().toRealPath()
                  require(Files.isRegularFile(jarpath), s"absolute jar path: $jarpath")
                  require(Files.isReadable(jarpath), s"absolute jar path: $jarpath")
                  jarpath.toUri.toURL
                } else {
                  val jarpath = scriptProjectLogicalPath.resolve(entry).normalize().toRealPath()
                  require(Files.isRegularFile(jarpath), s"$entry => resolved jar path: $jarpath")
                  require(Files.isReadable(jarpath), s"$entry => resolved jar path: $jarpath")
                  jarpath.toUri.toURL
                }
              } toSet

              val bins = classpathBinEntry.findAllMatchIn(classpathContent).flatMap { m =>
                val binpath = scriptProjectRealPath.resolve(m.group(1)).normalize().toRealPath()
                if (Files.isDirectory(binpath) && Files.isReadable(binpath))
                  Some(binpath.toUri.toURL)
                else
                  None
              } toSet

              val allJars = jars ++ cons

              if (bins.isEmpty && allJars.isEmpty)
                Failure( DynamicScriptsProjectEmptyClasspath( projectName, classpathFilepath ) )
              else
                Success(Some(URLPaths(bins, allJars).merge(urlPaths)))

            })
        else {

          // no '.classpath', try default paths...

          val scriptProjectBin = scriptProjectRealPath.resolve("bin").toFile
          val bins =
            if (!isFolderAvailable(scriptProjectBin))
              Set[URL]()
            else
              Set[URL](scriptProjectBin.toURI.toURL)

          val scriptProjectLib = scriptProjectRealPath.resolve("lib").toFile
          val jars =
            if (!isFolderAvailable(scriptProjectLib))
              Set[URL]()
            else
              TraversePath.listFilesRecursively(scriptProjectLib, jarFilenameFilter).map(_.toURI.toURL).toSet

          if (bins.isEmpty && jars.isEmpty)
            Failure(DynamicScriptsProjectMissingClasspath(projectName, classpathFilepath))
          else
            Success(Some(URLPaths(bins, jars).merge(urlPaths)))

        }

        classpathURLs
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
    val log = guiLog.getMDPluginsLog

    val init: Try[Some[URLPaths]] = resolveProjectPaths(Success(None), projectContext.project)
    val last: Try[Some[URLPaths]] = ( init /: projectContext.dependencies )( resolveProjectPaths )

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
      case Success( Some( urlPaths ) ) =>
        log.info(
          s"gov.nasa.jpl.dynamicScripts.magicdraw - " +
          s"ClassLoader with ${urlPaths.classpath.size} URLs: "+
          s"$s${urlPaths.classpath.mkString( "\n => ", "\n => ", "\n" )}" )
        Success( urlPaths.toURLClassLoader(parentClassLoader) )
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