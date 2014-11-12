package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class ReferenceNodeInfo( override val identifier: String, val e: Element, val primaryKey: String, val secondaryKey: String )
extends AbstractTreeNodeInfo( identifier ) {
  
  def comparePrimaryKey = primaryKey
  
  def compareSecondaryKey = secondaryKey
  
  def getAnnotations = Seq()
}