/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes

import javax.swing.Icon
import javax.swing.KeyStroke
import scala.language.implicitConversions
import scala.language.postfixOps
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.actions.DrawShapeDiagramAction
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import scala.util.Success
import scala.util.Failure

/**
 * @BUG 
 * 
 * Report to NoMagic a problem with the fact that the icon of a DrawShapeDiagramAction does not always show. 
 * This affects MD's diagram toolbar buttons as well (e.g., Class, Package, etc...)
 */

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DrawDynamicShapeAction(
    finalizationAction: DynamicShapeFinalizationAction,
    diagram: DiagramPresentationElement,
    ID: String, name: String, key: KeyStroke, largeIcon: Icon)
    extends DrawShapeDiagramAction(ID, name, key) {
  
  setDiagram(diagram)
  setLargeIcon(largeIcon)
  
  override def createElement(): Element = 
    finalizationAction.creatorHelper.createElement(Project.getProject(diagram)) match {
    case Success(e) => e
    case Failure(e) => throw e
  } 
  
  override def createPresentationElement(): PresentationElement = {
    val pe = super.createPresentationElement()
    pe
  }
  
  override def getCustomAdditionalDrawAction(): AdditionalDrawAction = {
    finalizationAction
  }
  
  override def clone(): Object = {
    val c = copy()
    c
  }
}