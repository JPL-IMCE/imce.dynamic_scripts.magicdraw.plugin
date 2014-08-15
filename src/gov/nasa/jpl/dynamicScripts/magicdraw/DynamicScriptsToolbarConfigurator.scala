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
package gov.nasa.jpl.dynamicScripts.magicdraw

import java.awt.event.ActionEvent

import com.nomagic.actions.AMConfigurator
import com.nomagic.actions.ActionsCategory
import com.nomagic.actions.ActionsManager
import com.nomagic.actions.NMAction
import com.nomagic.magicdraw.actions.ConfiguratorWithPriority
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.utils.MDLog

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicScriptsToolbarConfigurator() extends AMConfigurator {
  
  override def getPriority(): Int = ConfiguratorWithPriority.MEDIUM_PRIORITY
  
  override def configure(manager: ActionsManager): Unit = {
    val log = MDLog.getPluginsLog()
    val category = manager.getActionFor(DynamicScriptsToolbarConfigurator.CATEGORY_ID) match {
      case null => 
        log.info("DynamicScriptsToolbarConfigurator.configure (no manager")
        val c = new ActionsCategory(DynamicScriptsToolbarConfigurator.CATEGORY_ID, DynamicScriptsToolbarConfigurator.CATEGORY_NAME)
        c.setNested(true)
        manager.addCategory(c)
        c
      case c:ActionsCategory => 
        log.info("DynamicScriptsToolbarConfigurator.configure (with manager")
        c
    }
    
    category.addAction(new NMAction(DynamicScriptsToolbarConfigurator.RELOAD_ID, DynamicScriptsToolbarConfigurator.RELOAD_NAME, 0) {
      
      override def actionPerformed(ev: ActionEvent): Unit = {
        val p = DynamicScriptsPlugin.getInstance()
        val files = p.getDynamicScriptsOptions().getDynamicScriptConfigurationFiles()
        val message = p.updateRegistryForConfigurationFiles(files) match {
          case None => "\nSuccess"
          case Some(m) => "\n" + m
        }
        
        val guiLog = Application.getInstance().getGUILog()
        guiLog.log("=============================")
        guiLog.log(s"Reloaded ${files.size} dynamic script files:")
        files.foreach {f => guiLog.log("\n => " + f)}
        guiLog.log(message)
        guiLog.log("=============================")
      }
    })
  }
}

object DynamicScriptsToolbarConfigurator {
  
  val CATEGORY_ID: String = "DYNAMIC_SCRIPTS_TOOLBAR_CATEGORY"
  val CATEGORY_NAME: String = "DynamicScripts"
  
  val RELOAD_ID: String = "RELOAD_DYNAMIC_SCRIPTS_FILES"
  val RELOAD_NAME: String = "Reload DynamicScript Files"
  
}
