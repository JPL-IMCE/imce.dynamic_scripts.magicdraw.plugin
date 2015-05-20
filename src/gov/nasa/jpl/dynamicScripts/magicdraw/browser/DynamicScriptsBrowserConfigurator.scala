/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.browser

import java.awt.event.MouseEvent

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps

import com.nomagic.actions.AMConfigurator
import com.nomagic.actions.ActionsManager
import com.nomagic.magicdraw.actions.ConfiguratorWithPriority
import com.nomagic.magicdraw.actions.MDActionsCategory
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.ui.browser.Tree
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BrowserContextMenuAction
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.DynamicBrowserContextMenuActionForTriggerAndSelection
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper
import gov.nasa.jpl.magicdraw.enhanced.ui.browser.EnhancedBrowserContextAMConfigurator

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsBrowserConfigurator extends EnhancedBrowserContextAMConfigurator with AMConfigurator {

  override def getPriority(): Int = ConfiguratorWithPriority.HIGH_PRIORITY
  override def configure( manager: ActionsManager ): Unit = {}

  /**
   * This will be called by AspectJ advice...
   * @see gov.nasa.jpl.magicdraw.advice.ui.browser.EnhanceBrowserContextConfiguratorWithShowPopupMenuAdvice
   */
  override def configure( manager: ActionsManager, tree: Tree, mouseEvent: MouseEvent, trigger: Node, selection: java.util.Collection[Node] ): Unit = {

    val log = MDGUILogHelper.getMDPluginsLog
    val previousTime = System.currentTimeMillis()
      
    def getSelectedElement( node: Node ): Option[Element] = node.getUserObject() match {
      case e: Element => Some( e )
      case _          => None
    }

    getSelectedElement( trigger ) match {
      case None => ()
      case Some( e: Element ) =>
        if ( null == manager.getCategory( DynamicScriptsBrowserConfigurator.DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID ) ) {
          val category = new MDActionsCategory(
            DynamicScriptsBrowserConfigurator.DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID,
            DynamicScriptsBrowserConfigurator.DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_NAME )
          category.setNested( true )
          manager.addCategory( 0, category )

          configure( category, tree, mouseEvent, trigger, e, selection.toList flatMap getSelectedElement )
        }
    }

    val currentTime = System.currentTimeMillis()
    log.info( s"DynamicScriptsBrowserConfigurator.configure took ${currentTime - previousTime} ms" )
  }

  def configure( category: MDActionsCategory, tree: Tree, mouseEvent: MouseEvent, triggerNode: Node, triggerElement: Element, selected: List[Element] ): Unit = {

    val p = DynamicScriptsPlugin.getInstance()
    val mName = ClassTypes.getShortName( triggerElement.getClassType() )
    
    def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean = DynamicScriptsBrowserConfigurator.isDynamicContextMenuScriptActionAvailable( das )

    val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter )
    val sActions = StereotypesHelper.getStereotypesHierarchy( triggerElement ) flatMap ( s => 
      Option.apply(s.getProfile) match {
        case Some( pf ) => 
          p.getRelevantStereotypeActions(
              mName, pf.getQualifiedName(), s.getQualifiedName(), dynamicScriptMenuFilter )
        case None => 
          Map[String, Seq[DynamicActionScript]]()
      })
    val cActions = p.getRelevantClassifierActions( triggerElement, dynamicScriptMenuFilter )

    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = mActions ++ sActions ++ cActions
    val project = Project.getProject( triggerElement )
    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      menuActions = dynamicScriptActions flatMap DynamicScriptsBrowserConfigurator.createContextMenuActionForTriggerAndSelection( project, tree, triggerNode, triggerElement, selected )
      if ( menuActions.nonEmpty )
    } {
      val subCategory = new MDActionsCategory( key, key )
      subCategory.setEnabled( true )
      subCategory.setNested( true )
      subCategory.setActions( menuActions )
      category.addAction( subCategory )
    }

  }

}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsBrowserConfigurator {

  val DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID: String = "DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID"
  val DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_NAME: String = "DynamicScriptsContextMenu"

  def isDynamicContextMenuScriptActionAvailable( das: DynamicActionScript ): Boolean = das match {
    case c: BrowserContextMenuAction => ClassLoaderHelper.isDynamicActionScriptAvailable( c )
    case _                           => false
  }

  def createContextMenuActionForTriggerAndSelection( project: Project, tree: Tree, triggerNode: Node, triggerElement: Element, selected: List[Element] )( dynamicActionScript: DynamicActionScript ): Option[DynamicBrowserContextMenuActionForTriggerAndSelection] =
    dynamicActionScript match {
      case s: BrowserContextMenuAction => Some( DynamicBrowserContextMenuActionForTriggerAndSelection( project, tree, triggerNode, triggerElement, selected, s, null, null ) )
      case _                           => None
    }
}