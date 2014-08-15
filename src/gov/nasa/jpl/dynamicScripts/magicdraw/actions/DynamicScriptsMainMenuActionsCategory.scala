/**
 * License Terms
 *
 * Copyright (c) 2014, California
 * Institute of Technology ("Caltech").  U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *
 *  *   Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *  *   Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the
 *      distribution.
 *
 *  *   Neither the name of Caltech nor its operating division, the Jet
 *      Propulsion Laboratory, nor the names of its contributors may be
 *      used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import javax.swing.KeyStroke

import scala.language.postfixOps

import com.nomagic.actions.ActionsCategory
import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.actions.MDActionsCategory
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.utils.MDLog

import gov.nasa.jpl.dynamicScripts.DynamicScriptsRegistry
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicScriptsForMainToolbarMenus
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.HName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MainToolbarMenuAction
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicScriptsMainMenuActionsCategory() extends ActionsCategory(
  DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_ID,
  DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_NAME ) {

  val refreshAction = new NMAction( 
      DynamicScriptsMainMenuActionsCategory.RELOAD_ID, 
      DynamicScriptsMainMenuActionsCategory.RELOAD_NAME, 
      null.asInstanceOf[KeyStroke] ) {

      override def actionPerformed( ev: ActionEvent ): Unit = {
        val p = DynamicScriptsPlugin.getInstance()
        val files = p.getDynamicScriptsOptions().getDynamicScriptConfigurationFiles()
        val message = p.updateRegistryForConfigurationFiles( files ) match {
          case None => doRefresh()
            "\nSuccess"
          case Some( m ) =>
            "\n" + m
        }

        val guiLog = Application.getInstance().getGUILog()
        guiLog.log( "=============================" )
        guiLog.log( s"Reloaded ${files.size} dynamic script files:" )
        files.foreach { f => guiLog.log( "\n => " + f ) }
        guiLog.log( message )
        guiLog.log( "=============================" )
      }
    }
  
  setNested(true)
  addAction( refreshAction )
  
  override def updateState(): Unit = {}

  def doRefresh(): Unit = {
    val log = MDLog.getPluginsLog()
    log.info(s"*** DynamicScriptsMainMenuActionsCategory.doRefresh()")
    var it = getActions().iterator()
    while ( it.hasNext() ) { 
      val action = it.next()
      if (refreshAction != action) {
        removeAction( action )
      }
    }

    val reg: DynamicScriptsRegistry = DynamicScriptsPlugin.getInstance().getDynamicScriptsRegistry()
    reg.toolbarMenuPathActions.values.flatten map addMainToolbarMenuActions

    log.info(s"*** DynamicScriptsMainMenuActionsCategory.doRefresh() - done")
  }
  
  def addMainToolbarMenuActions( menu: DynamicScriptsForMainToolbarMenus ): Unit = menu.scripts foreach ( addMainToolbarMenuScript( menu.name, _ ))
    
  def addMainToolbarMenuScript( menuName: HName, script: MainToolbarMenuAction ): Unit = {
    val menuCategory = getOrCreateSubMenuFor( script.toolbarMenuPath )
    menuCategory.addAction( DynamicScriptsLaunchToolbarMenuAction( script, s"${menuCategory.getID()}_=>_${script.name.hname}" ))
  }
  
  def getOrCreateSubMenuFor( path: Seq[HName] ): ActionsCategory = {
    val r: ActionsCategory = ( this.asInstanceOf[ActionsCategory] /: path ) (getOrCreateSubMenuForPathSegment(_,_))
    r
  }
    
  def getOrCreateSubMenuForPathSegment( category: ActionsCategory, segment: HName ): ActionsCategory = 
    category.getAction(segment.hname) match {
      case subCategory: ActionsCategory => subCategory
      case null => {
        val subCategory = new MDActionsCategory( s"${category.getID()}_|_${segment.hname}", segment.hname )
        subCategory.setNested(true)
        category.addAction(subCategory)
        subCategory
      }
      case _ => throw new IllegalArgumentException(s"DynamicScriptsMainMenuActionsCategory.getOrCreateSubMenuForPathSegment - category: ${category.getID()} / ${category.getName()}; segment=${segment}")
    }
    
}

object DynamicScriptsMainMenuActionsCategory {

  val DYNAMIC_SCRIPTS_MENU_NAME: String = "Dynamic Scripts"
  val DYNAMIC_SCRIPTS_MENU_ID: String = "IMCE_DYNAMIC_SCRIPTS_MENU_ID"

  val RELOAD_ID: String = "RELOAD_DYNAMIC_SCRIPTS_FILES"
  val RELOAD_NAME: String = "Reload DynamicScript files..."
}
