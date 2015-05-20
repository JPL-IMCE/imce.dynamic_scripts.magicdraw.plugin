/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.scripts

import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicShapeCreatorScriptForStereotypedClassifiedInstanceDesignation {

  def postCreateCallback(das: DynamicActionScript, r: MagicDrawStereotypedClassifiedInstanceDesignation, pe: PresentationElement, e: Element): Boolean = {
    val log = MDGUILogHelper.getMDPluginsLog
    log.info(s"""|DynamicShapeCreatorScriptForStereotypedClassifiedInstanceDesignation.postCreateCallback
                 |- action : ${das.prettyPrint("  ")}
                 |- element: ${e.getHumanType()}: ${e.getID()}
                 |- shape  : ${pe.getHumanType()}
                 |""".stripMargin)
    true
  }
    
}