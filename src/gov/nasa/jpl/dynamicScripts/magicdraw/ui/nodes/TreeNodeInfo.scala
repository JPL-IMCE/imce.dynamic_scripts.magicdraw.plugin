/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class TreeNodeInfo( 
    override val identifier: String, 
    val nested: Seq[(AbstractTreeNodeInfo, Map[String, AbstractTreeNodeInfo])] = Seq(),
    val annotations: Seq[ValidationAnnotation] = Seq() )
extends AbstractTreeNodeInfo( identifier ) {
  
  val compareKey = identifier
  
  def getAnnotations = annotations
}

object TreeNodeInfo {  
  
  def isTable( o: Object ): Boolean = o match {
    case _: TreeNodeInfo => true
    case _ => false
  }
}