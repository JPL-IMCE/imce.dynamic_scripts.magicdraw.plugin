/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions

import java.awt.event.ActionEvent
import java.net.URLClassLoader

import javax.swing.KeyStroke

import scala.language.implicitConversions
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Success

import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MainToolbarMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper.ResolvedClassAndMethod

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicScriptsLaunchToolbarMenuAction( action: MainToolbarMenuAction, id: String )
  extends NMAction( id, action.name.hname, null.asInstanceOf[KeyStroke] ) {

  override def getDescription(): String =
    action.prettyPrint("  ")
    
  override def actionPerformed( ev: ActionEvent ): Unit = {
    val previousTime = System.currentTimeMillis()
    val message = action.prettyPrint( "" ) + "\n"

    ClassLoaderHelper.createDynamicScriptClassLoader( action ) match {
      case Failure( t ) =>
        ClassLoaderHelper.reportError( action, message, t )
        return

      case Success( scriptCL: URLClassLoader ) => {
        val localClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader( scriptCL )

        try {
          ClassLoaderHelper.lookupClassAndMethod( scriptCL, action,
            classOf[Project], classOf[ActionEvent], classOf[MainToolbarMenuAction] ) match {
              case Failure( t ) =>
                ClassLoaderHelper.reportError( action, message, t )
                return

              case Success( cm: ResolvedClassAndMethod ) =>
                ClassLoaderHelper.invokeAndReport( previousTime, Application.getInstance().getProject(), ev, cm )
            }
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }
}