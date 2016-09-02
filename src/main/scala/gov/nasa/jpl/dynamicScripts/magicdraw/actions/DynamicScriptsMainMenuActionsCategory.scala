/*
 * Copyright 2014 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * License Terms
 */

package gov.nasa.jpl.dynamicScripts.magicdraw.actions

import java.awt.event.ActionEvent
import java.lang.IllegalArgumentException

import javax.swing.KeyStroke
import com.nomagic.actions.ActionsCategory
import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.actions.MDActionsCategory
import gov.nasa.jpl.dynamicScripts.DynamicScriptsRegistry
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicScriptsForMainToolbarMenus
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.HName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MainToolbarMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

import scala.collection.immutable._
import scala.{Option, StringContext, Unit}
import scala.Predef.String
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicScriptsMainMenuActionsCategory()
  extends ActionsCategory(
    DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_ID,
    DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_NAME )
  with RefreshableActionsCategory {

  val refreshAction = new NMAction(
    DynamicScriptsMainMenuActionsCategory.RELOAD_ID,
    DynamicScriptsMainMenuActionsCategory.RELOAD_NAME,
    null.asInstanceOf[KeyStroke] ) {

    override def actionPerformed( ev: ActionEvent ): Unit =
      DynamicScriptsPlugin.getInstance().loadDynamicScriptsFiles()

  }

  val showAction = ShowDynamicScripts()

  val dsActions = Set( showAction, refreshAction )

  setNested( true )
  addAction( showAction )
  addAction( refreshAction )

  override def updateState(): Unit = {}

  override def doRefresh(): Unit = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog

    log.info( s"*** DynamicScriptsMainMenuActionsCategory.doRefresh()" )
    val it = getActions.iterator()
    while ( it.hasNext ) {
      val action = it.next()
      if ( !dsActions.contains(action) ) {
        removeAction( action )
      }
    }

    val reg: DynamicScriptsRegistry = DynamicScriptsPlugin.getInstance().getDynamicScriptsRegistry
    reg.toolbarMenuPathActions foreach {
      case ( ( _: String, menus: scala.collection.immutable.SortedSet[DynamicScriptsForMainToolbarMenus] ) ) =>
        menus foreach addMainToolbarMenuActions
    }

    log.info( s"*** DynamicScriptsMainMenuActionsCategory.doRefresh() - done" )
  }

  def addMainToolbarMenuActions
  ( menu: DynamicScriptsForMainToolbarMenus )
  : Unit =
    menu.scripts foreach ( addMainToolbarMenuScript( menu.name, _ ) )

  def addMainToolbarMenuScript
  ( menuName: HName, script: MainToolbarMenuAction )
  : Unit = {
    val menuCategory = getOrCreateSubMenuFor( script.toolbarMenuPath )
    menuCategory.addAction( DynamicScriptsLaunchToolbarMenuAction(
      script, s"${menuCategory.getID}_=>_${script.name.hname}" ) )
  }

  def getOrCreateSubMenuFor
  ( path: Seq[HName] )
  : ActionsCategory = {
    val r: ActionsCategory = ( this.asInstanceOf[ActionsCategory] /: path )( getOrCreateSubMenuForPathSegment )
    r
  }

  def getOrCreateSubMenuForPathSegment
  ( category: ActionsCategory, segment: HName )
  : ActionsCategory = {
    val segmentID = s"${category.getID}_|_${segment.hname}"
    Option.apply(category.getAction( segmentID ))
    .fold[ActionsCategory]({
      val subCategory = new MDActionsCategory(segmentID, segment.hname)
      subCategory.setNested(true)
      category.addAction(subCategory)
      subCategory
    }){
      case subCategory: ActionsCategory =>
        subCategory
      case _ =>
        throw new IllegalArgumentException(
          "DynamicScriptsMainMenuActionsCategory.getOrCreateSubMenuForPathSegment - category: "+
          s"${category.getID} / ${category.getName}; segment=$segment" )
    }
  }
}

object DynamicScriptsMainMenuActionsCategory {

  val DYNAMIC_SCRIPTS_MENU_NAME: String = "Dynamic Scripts"
  val DYNAMIC_SCRIPTS_MENU_ID: String = "IMCE_DYNAMIC_SCRIPTS_MENU_ID"

  val RELOAD_ID: String = "RELOAD_DYNAMIC_SCRIPTS_FILES"
  val RELOAD_NAME: String = "Reload DynamicScript files..."
}