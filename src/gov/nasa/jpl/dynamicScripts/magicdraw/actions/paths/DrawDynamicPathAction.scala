/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths

import javax.swing.Icon
import javax.swing.KeyStroke
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.magicdraw.uml.symbols.paths.PathElement
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction
import scala.util.Success
import scala.util.Failure

/**
 * Normally, this class would extend: com.nomagic.magicdraw.ui.actions.DrawPathDiagramAction
 * Instead, it extends: gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction
 * The difference between them is in the AspectJ wormhole pattern used 
 * to inject the large icon when creating the action's com.nomagic.magicdraw.ui.states.SymbolDrawState.
 * 
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 * @see gov.nasa.jpl.magicdraw.enhanced.actions.paths.EnhancedDrawPathAction
 * @see gov.nasa.jpl.magicdraw.advice.actions.paths.EnhancedDrawDynamicPathActionCreateState
 */
case class DrawDynamicPathAction(
    finalizationAction: DynamicPathFinalizationAction,
    diagram: DiagramPresentationElement,
    ID: String, name: String, key: KeyStroke, largeIcon: Icon)
    extends EnhancedDrawPathAction(finalizationAction, diagram, ID, name, key, largeIcon) {
  
  override def createElement(): Element = 
    finalizationAction.creatorHelper.createElement(Project.getProject(diagram)) match {
    case Success(e) => e
    case Failure(e) => throw e
  }
  
  override def createPathElement(): PathElement = 
    createElement() match {
    case null => null
    case e: Element => finalizationAction.creatorHelper.createPathElement(e)
  }
  
  override def createAdditionalDrawAction(): AdditionalDrawAction = finalizationAction
  
  override def clone(): Object = copy()
}