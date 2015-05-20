/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions

import java.awt.event.ActionEvent
import java.net.URLClassLoader
import javax.swing.KeyStroke

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Success

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.ui.browser.Tree
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction
import com.nomagic.magicdraw.uml.BaseElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BrowserContextMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper.ResolvedClassAndMethod
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicBrowserContextMenuActionForTriggerAndSelection(
  project: Project, tree: Tree, triggerNode: Node, triggerElement: Element, selected: java.util.Collection[Element],
  menuAction: BrowserContextMenuAction,
  key: KeyStroke,
  group: String ) extends DefaultBrowserAction( menuAction.name.hname, menuAction.name.hname, key, group ) {

  override def getTree(): Tree = tree
  override def getFirstElement(): BaseElement = triggerElement
  override def getSelectedObject(): Object = triggerElement
  override def getSelectedObjects(): java.util.Collection[_] = selected

  override def toString(): String =
    s"${menuAction.name.hname}"

  override def getDescription(): String =
    menuAction.prettyPrint("  ")
    
  override def updateState(): Unit = {
    super.updateState()
    setEnabled( ClassLoaderHelper.isDynamicActionScriptAvailable( menuAction ) && MDUML.isAccessCompatibleWithElements( menuAction.access, ( triggerElement :: selected.toList) : _*))
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
          ClassLoaderHelper.lookupClassAndMethod( 
              scriptCL, menuAction, 
              classOf[Project], classOf[ActionEvent],
              classOf[BrowserContextMenuAction], 
              classOf[Tree], classOf[Node], 
              classOf[Element], 
              classOf[java.util.Collection[Element]] ) match {
            case Failure( t1 ) =>
              ClassLoaderHelper.lookupClassAndMethod( 
                  scriptCL, menuAction, 
                  classOf[Project], classOf[ActionEvent],
                  classOf[BrowserContextMenuAction], 
                  classOf[Tree], classOf[Node], 
                  triggerElement.getClassType(), 
                  classOf[java.util.Collection[Element]] ) match {
                case Failure( t2 ) =>
                  ClassLoaderHelper.reportError( menuAction, message, t1 )
                  return

                case Success( cm2: ResolvedClassAndMethod ) =>
                  ClassLoaderHelper.invokeAndReport( previousTime, project, ev, cm2, tree, triggerNode, triggerElement, selected )
              }

            case Success( cm1: ResolvedClassAndMethod ) =>
              ClassLoaderHelper.invokeAndReport( previousTime, project, ev, cm1, tree, triggerNode, triggerElement, selected )
          }
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }
}