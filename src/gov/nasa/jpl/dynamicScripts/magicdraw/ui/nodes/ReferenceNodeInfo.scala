package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class ReferenceNodeInfo( override val identifier: String, val e: Element, annotations: Seq[ValidationAnnotation] = Seq() )
extends AbstractTreeNodeInfo( identifier ) {
  
  val compareKey: String = identifier + e.getHumanType + e.getID

  def getAnnotations = annotations
}