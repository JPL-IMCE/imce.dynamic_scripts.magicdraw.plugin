/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes

import java.awt.event.ActionEvent

import javax.swing.Icon

import com.nomagic.magicdraw.ui.actions.DiagramContextToolbarAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicShapeDiagramContextToolbarAction(
    diagram: DiagramPresentationElement,
    finalizationAction: DynamicShapeFinalizationAction) 
    extends DiagramContextToolbarAction(
        finalizationAction.action.name.hname, 
        finalizationAction.action.name.hname, 
        DynamicScriptsPlugin.getInstance().getDynamicScriptIcon(finalizationAction.action)) { 
  
  override def updateState(): Unit = {
    super.updateState()
    setEnabled(finalizationAction.isEnabled() && MDUML.isAccessCompatibleWithElements( finalizationAction.action.access, diagram ))
  }
  
  override def getDescription(): String =
    finalizationAction.action.prettyPrint("  ")
    
  override def actionPerformed(ev: ActionEvent): Unit = {
    val drawRelatinshipPathAction = DrawDynamicShapeAction(finalizationAction, getDiagram(), getID() + ".DrawShapeAction", getName(), null, getLargeIcon())
    drawRelatinshipPathAction.actionPerformed(ev)
  }
  
  override def getSmallIcon(): Icon = getLargeIcon()
  
  override def getDiagram(): DiagramPresentationElement = diagram
  
  override def clone(): Object = copy()
  
}