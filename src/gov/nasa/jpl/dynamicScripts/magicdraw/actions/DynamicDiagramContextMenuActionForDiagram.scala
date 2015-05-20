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

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.actions.DefaultDiagramAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DiagramContextMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper.ResolvedClassAndMethod
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicDiagramContextMenuActionForDiagram(
  project: Project, diagram: DiagramPresentationElement,
  menuAction: DiagramContextMenuAction,
  key: KeyStroke,
  group: String ) extends DefaultDiagramAction( menuAction.name.hname, menuAction.name.hname, key, group ) {

  override def toString(): String =
    s"${menuAction.name.hname}"

  override def getDescription(): String =
    menuAction.prettyPrint("  ")
    
  override def updateState(): Unit = {
    super.updateState()
    setEnabled( ClassLoaderHelper.isDynamicActionScriptAvailable( menuAction ) && MDUML.isAccessCompatibleWithElements( menuAction.access, diagram ))
  }

  override def actionPerformed( ev: ActionEvent ): Unit = {
    val previousTime = System.currentTimeMillis()
    val message = menuAction.prettyPrint( "" ) + "\n"

    ClassLoaderHelper.createDynamicScriptClassLoader( menuAction ) match {
      case Failure( t ) =>
        ClassLoaderHelper.reportError( menuAction, message, t )
        return

      case Success( scriptCL: URLClassLoader ) => {
        val localClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader( scriptCL )

        try {
          ClassLoaderHelper.lookupClassAndMethod( scriptCL, menuAction, 
              classOf[Project], classOf[ActionEvent], classOf[DiagramContextMenuAction], classOf[DiagramPresentationElement] ) match {
            case Failure( t ) =>
              ClassLoaderHelper.reportError( menuAction, message, t )
              return

            case Success( cm: ResolvedClassAndMethod ) =>
              ClassLoaderHelper.invokeAndReport( previousTime, project, ev, cm, diagram )
          }
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }
}