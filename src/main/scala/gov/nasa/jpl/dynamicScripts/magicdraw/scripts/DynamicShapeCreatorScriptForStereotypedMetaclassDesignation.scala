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

package gov.nasa.jpl.dynamicScripts.magicdraw.scripts

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

import scala.{Boolean, StringContext}
import scala.Predef.augmentString

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicShapeCreatorScriptForStereotypedMetaclassDesignation {

  def postCreateCallback
  (das: DynamicActionScript, r: ResolvedMagicDrawStereotypeDesignation, pe: PresentationElement, e: Element)
  : Boolean = {
    val guiLog = Application.getInstance.getGUILog
    import MDGUILogHelper._
    val log = guiLog.getMDPluginsLog
    log.info(s"""|DynamicShapeCreatorScriptForStereotypedMetaclassDesignation.postCreateCallback
                 |- action : ${das.prettyPrint("  ")}
                 |- element: ${e.getHumanType}: ${e.getID}
                 |- shape  : ${pe.getHumanType}
                 |""".stripMargin)
    true
  }
    
}