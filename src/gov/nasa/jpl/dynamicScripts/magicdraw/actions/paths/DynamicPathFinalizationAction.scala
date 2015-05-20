/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths

import java.awt.Point
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException
import scala.language.implicitConversions
import scala.util.Failure
import scala.util.Success
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager
import com.nomagic.utils.Utilities
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelPathInstanceCreator
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.MagicDrawValidationDataResults
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.UncaughtExceptionHandler

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicPathFinalizationAction(
  val action: ToplevelPathInstanceCreator,
  val creatorHelper: DynamicPathCreatorHelper )
  extends AdditionalDrawAction {

  def getSortKey(): String = action.sortKey()

  def isEnabled(): Boolean =
    ClassLoaderHelper.isDynamicActionScriptAvailable( action ) && creatorHelper.isResolved

  override def execute( pe: PresentationElement, point: Point ): Boolean = {
    val log = MDGUILogHelper.getMDPluginsLog
    val e = pe.getElement()

    val previousTime = System.currentTimeMillis()

    val message = action.prettyPrint( "" )
    val guiLog = Application.getInstance().getGUILog()

    UncaughtExceptionHandler( message )
    ClassLoaderHelper.createDynamicScriptClassLoader( action ) match {
      case Failure( ex ) =>
        val error = "${message}: project not found '${menuAction.projectName.jname}'"
        log.error( error )
        guiLog.showError( error )
        return false

      case Success( scriptCL ) => {
        val localClassLoader = Thread.currentThread().getContextClassLoader()
        try {

          Thread.currentThread().setContextClassLoader( scriptCL )

          val c = scriptCL.loadClass( action.className.jname )
          if ( c == null ) {
            val error = "${message}: class '${menuAction.className.jname}' not found in project '${menuAction.projectName.jname}'"
            log.error( error )
            guiLog.showError( error )
            return false
          }

          val m = creatorHelper.lookupMethod( c, action ) match {
            case Failure( t ) =>
              val error = s"${message}: ${t.getMessage()}"
              log.error( error, t )
              guiLog.showError( error, t )
              return false
            case Success( m ) => m
          }

          val r = creatorHelper.invokeMethod( m, action, pe, point, e )

          val currentTime = System.currentTimeMillis()
          log.info( s"${message} took ${currentTime - previousTime} ms" )

          r match {
            case Failure( ex ) =>
              val ex_message = message + s"\n${ex.getMessage()}"
              log.error( ex_message, ex )
              guiLog.showError( ex_message, ex )
              false

            case Success( None ) =>
              true

            case Success( Some( MagicDrawValidationDataResults( title, runData, results, postSessionActions ) ) ) =>
              if ( !results.isEmpty() )
                Utilities.invokeAndWaitOnDispatcher( new Runnable() {
                  override def run(): Unit = {
                    ValidationResultsWindowManager.updateValidationResultsWindow( currentTime.toString(), title, runData, results )
                  }
                } )
              if ( !postSessionActions.isEmpty() )
                guiLog.showError( s"There are ${postSessionActions.size()} post-session actions that will not be executed because session management is not accessible for MD shape finalization actions" )
              false

            case b: java.lang.Boolean => b.booleanValue()

            case _                    => false
          }

        }
        catch {
          case ex: InvocationTargetException =>
            val t = ex.getTargetException() match { case null => ex; case t => t }
            val ex_message = message + s"\n${t.getMessage()}"
            log.error( ex_message, t )
            guiLog.showError( ex_message, t )
            return false
          case ex @ ( _: ClassNotFoundException | _: SecurityException | _: NoSuchMethodException | _: IllegalArgumentException | _: IllegalAccessException | _: MalformedURLException | _: NoSuchMethodException ) =>
            val ex_message = message + s"\n${ex.getMessage()}"
            log.error( ex_message, ex )
            guiLog.showError( ex_message, ex )
            return false
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }

  override def afterExecute( pe: PresentationElement, point: Point ): Unit = {}

}