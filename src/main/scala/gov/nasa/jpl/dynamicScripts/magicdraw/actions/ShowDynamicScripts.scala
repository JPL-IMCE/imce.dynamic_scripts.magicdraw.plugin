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
import javax.swing.KeyStroke

import com.nomagic.actions.NMAction
import gov.nasa.jpl.dynamicScripts.DynamicScriptsRegistry
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

import scala.Predef.String
import scala.Unit

case class ShowDynamicScripts()
  extends NMAction(ShowDynamicScripts.SHOW_ID, ShowDynamicScripts.SHOW_NAME, null.asInstanceOf[KeyStroke] ) {

  override def actionPerformed( ev: ActionEvent ): Unit = {
    val reg: DynamicScriptsRegistry = DynamicScriptsPlugin.getInstance().getDynamicScriptsRegistry

    import MDGUILogHelper._
    val guiLog = getGUILog
    guiLog.openLog()
    guiLog.log("See the MD Log for the dump of the Dynamic Scripts registry")
    guiLog.getMDPluginsLog.info(reg.toString)

  }
}

object ShowDynamicScripts {

  val SHOW_ID: String = "SHOW_DYNAMIC_SCRIPTS"
  val SHOW_NAME: String = "Show DynamicScripts..."

}