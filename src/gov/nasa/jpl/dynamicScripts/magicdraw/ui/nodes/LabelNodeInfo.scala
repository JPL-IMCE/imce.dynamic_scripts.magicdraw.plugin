/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class LabelNodeInfo( val label: String, annotations: Seq[ValidationAnnotation] = Seq() )
  extends AbstractTreeNodeInfo( label ) {

  if (label == null || label.length == 0)
    throw new IllegalArgumentException("Label must be a non-empty String!")
  
  val compareKey = label
  
  def getAnnotations = annotations
}