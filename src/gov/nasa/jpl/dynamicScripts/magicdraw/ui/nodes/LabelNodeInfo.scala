package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class LabelNodeInfo( val label: String )
  extends AbstractTreeNodeInfo( label ) {

  if (label == null || label.length == 0)
    throw new IllegalArgumentException("Label must be a non-empty String!")
  
  def comparePrimaryKey = label
  def compareSecondaryKey = label
  
  def getAnnotations = Seq()
}