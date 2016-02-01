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

  setNested( true )
  addAction( refreshAction )

  override def updateState(): Unit = {}

  override def doRefresh(): Unit = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog()

    log.info( s"*** DynamicScriptsMainMenuActionsCategory.doRefresh()" )
    val it = getActions.iterator()
    while ( it.hasNext ) {
      val action = it.next()
      if ( refreshAction != action ) {
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