/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions

import java.awt.event.ActionEvent
import javax.swing.KeyStroke
import scala.language.postfixOps
import com.nomagic.actions.ActionsCategory
import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.actions.MDActionsCategory
import com.nomagic.magicdraw.utils.MDLog
import gov.nasa.jpl.dynamicScripts.DynamicScriptsRegistry
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicScriptsForMainToolbarMenus
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.HName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MainToolbarMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicScriptsMainMenuActionsCategory() extends ActionsCategory(
  DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_ID,
  DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_NAME ) with RefreshableActionsCategory {

  val refreshAction = new NMAction(
    DynamicScriptsMainMenuActionsCategory.RELOAD_ID,
    DynamicScriptsMainMenuActionsCategory.RELOAD_NAME,
    null.asInstanceOf[KeyStroke] ) {

    override def actionPerformed( ev: ActionEvent ): Unit =
      DynamicScriptsPlugin.getInstance().loadDynamicScriptsFiles()

  }

  setNested( true )
  addAction( refreshAction )

  override def updateState(): Unit = {}

  override def doRefresh(): Unit = {
    val log = MDGUILogHelper.getMDPluginsLog
    log.info( s"*** DynamicScriptsMainMenuActionsCategory.doRefresh()" )
    var it = getActions().iterator()
    while ( it.hasNext() ) {
      val action = it.next()
      if ( refreshAction != action ) {
        removeAction( action )
      }
    }

    val reg: DynamicScriptsRegistry = DynamicScriptsPlugin.getInstance().getDynamicScriptsRegistry()
    reg.toolbarMenuPathActions foreach {
      case ( ( _: String, menus: scala.collection.immutable.SortedSet[DynamicScriptsForMainToolbarMenus] ) ) =>
        menus foreach ( addMainToolbarMenuActions( _ ) )
    }

    log.info( s"*** DynamicScriptsMainMenuActionsCategory.doRefresh() - done" )
  }

  def addMainToolbarMenuActions( menu: DynamicScriptsForMainToolbarMenus ): Unit = menu.scripts foreach ( addMainToolbarMenuScript( menu.name, _ ) )

  def addMainToolbarMenuScript( menuName: HName, script: MainToolbarMenuAction ): Unit = {
    val menuCategory = getOrCreateSubMenuFor( script.toolbarMenuPath )
    menuCategory.addAction( DynamicScriptsLaunchToolbarMenuAction( script, s"${menuCategory.getID()}_=>_${script.name.hname}" ) )
  }

  def getOrCreateSubMenuFor( path: Seq[HName] ): ActionsCategory = {
    val r: ActionsCategory = ( this.asInstanceOf[ActionsCategory] /: path )( getOrCreateSubMenuForPathSegment( _, _ ) )
    r
  }

  def getOrCreateSubMenuForPathSegment( category: ActionsCategory, segment: HName ): ActionsCategory = {
    val segmentID = s"${category.getID()}_|_${segment.hname}"
    category.getAction( segmentID ) match {
      case null => {
        val subCategory = new MDActionsCategory( segmentID, segment.hname )
        subCategory.setNested( true )
        category.addAction( subCategory )
        subCategory
      }
      case subCategory: ActionsCategory =>
        subCategory
      case _ =>
        throw new IllegalArgumentException( s"DynamicScriptsMainMenuActionsCategory.getOrCreateSubMenuForPathSegment - category: ${category.getID()} / ${category.getName()}; segment=${segment}" )
    }
  }
}

object DynamicScriptsMainMenuActionsCategory {

  val DYNAMIC_SCRIPTS_MENU_NAME: String = "Dynamic Scripts"
  val DYNAMIC_SCRIPTS_MENU_ID: String = "IMCE_DYNAMIC_SCRIPTS_MENU_ID"

  val RELOAD_ID: String = "RELOAD_DYNAMIC_SCRIPTS_FILES"
  val RELOAD_NAME: String = "Reload DynamicScript files..."
}