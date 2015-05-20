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
import com.nomagic.magicdraw.ui.actions.DefaultDiagramAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DiagramContextMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper.ResolvedClassAndMethod
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicDiagramContextMenuActionForTriggerAndSelection(
  project: Project, 
  diagram: DiagramPresentationElement,
  trigger: PresentationElement, element: Element, selected: java.util.Collection[PresentationElement],
  menuAction: DiagramContextMenuAction,
  key: KeyStroke,
  group: String ) extends DefaultDiagramAction( menuAction.name.hname, menuAction.name.hname, key, group ) {

  override def toString(): String =
    s"${menuAction.name.hname}"

  override def getDescription(): String =
    menuAction.prettyPrint("  ")
    
  override def updateState(): Unit = {
    super.updateState()
    setEnabled( ClassLoaderHelper.isDynamicActionScriptAvailable( menuAction ) && MDUML.isAccessCompatibleWithElements( menuAction.access, ( diagram :: trigger :: element :: selected.toList) : _*))
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
              classOf[DiagramContextMenuAction], classOf[DiagramPresentationElement], 
              classOf[PresentationElement], classOf[Element], 
              classOf[java.util.Collection[PresentationElement]] ) match {
            case Failure( t1 ) =>
              ClassLoaderHelper.lookupClassAndMethod( 
                scriptCL, menuAction, 
                classOf[Project], classOf[ActionEvent], 
                classOf[DiagramContextMenuAction], classOf[DiagramPresentationElement], 
                trigger.getClassType(), element.getClassType(), 
                classOf[java.util.Collection[PresentationElement]] ) match {
                case Failure( t2 ) =>
                  ClassLoaderHelper.reportError( menuAction, message, t1 )
                  return
                case Success( cm2: ResolvedClassAndMethod ) =>
                 ClassLoaderHelper.invokeAndReport( previousTime, project, ev, cm2, diagram, trigger, element, selected )
              }
            case Success( cm1: ResolvedClassAndMethod ) =>
              ClassLoaderHelper.invokeAndReport( previousTime, project, ev, cm1, diagram, trigger, element, selected )
          }
        }
        finally {
          Thread.currentThread().setContextClassLoader( localClassLoader )
        }
      }
    }
  }
}