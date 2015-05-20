/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths

import java.awt.event.ActionEvent

import javax.swing.Icon

import com.nomagic.magicdraw.ui.actions.DiagramContextToolbarAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicPathDiagramContextToolbarAction(
    diagram: DiagramPresentationElement,
    firstSelected: PresentationElement,
    finalizationAction: DynamicPathFinalizationAction) 
    extends DiagramContextToolbarAction(
        finalizationAction.action.name.hname, 
        finalizationAction.action.name.hname, 
        DynamicScriptsPlugin.getInstance().getDynamicScriptIcon(finalizationAction.action)) { 
  
  setFakeMouseEvents(true)
  
  override def updateState(): Unit = {
    super.updateState()
    setEnabled(finalizationAction.isEnabled() && MDUML.isAccessCompatibleWithElements( finalizationAction.action.access, diagram, firstSelected ))
  }
  
  override def getDescription(): String =
    finalizationAction.action.prettyPrint("  ")
    
  override def actionPerformed(ev: ActionEvent): Unit = {
    val drawRelatinshipPathAction = DrawDynamicPathAction(finalizationAction, getDiagram(), getID() + ".DrawPathAction", getName(), null, getLargeIcon())
    drawRelatinshipPathAction.actionPerformed(ev)
    super.actionPerformed(ev)
  }
  
  override def getSmallIcon(): Icon = getLargeIcon()
  
  override def getFirstSelected(): PresentationElement = firstSelected
  
  override def clone(): Object = copy()
  
}