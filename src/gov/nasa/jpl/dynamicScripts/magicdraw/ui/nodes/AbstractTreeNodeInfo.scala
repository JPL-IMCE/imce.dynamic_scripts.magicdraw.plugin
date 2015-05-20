/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import javax.swing.tree.DefaultMutableTreeNode
import com.jidesoft.comparator.AlphanumComparator
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
abstract class AbstractTreeNodeInfo( val identifier: String )
  extends DefaultMutableTreeNode( identifier )
  with Comparable[AbstractTreeNodeInfo] {

  def dispose: Unit = {}

  val compareKey: String
  
  override def compareTo( o: AbstractTreeNodeInfo ): Int =
    AbstractTreeNodeInfo.expandSIPrefixes( this.compareKey ) compareTo
      AbstractTreeNodeInfo.expandSIPrefixes( o.compareKey )
      
  def getAnnotations: Seq[ValidationAnnotation]
}

object AbstractTreeNodeInfo {

  def collectAnnotationsRecursively( info: AbstractTreeNodeInfo, rows: Map[String, AbstractTreeNodeInfo] ): Iterable[ValidationAnnotation] = 
    ( Iterable(info) ++ rows.values ) flatMap ( collectAnnotationsRecursively( _ ) )
    
  def collectAnnotationsRecursively( info: AbstractTreeNodeInfo ): Iterable[ValidationAnnotation] = info match {
    case i: AnnotationNodeInfo => 
      i.getAnnotations 
    case i: LabelNodeInfo => 
      i.getAnnotations
    case i: ReferenceNodeInfo => 
      i.getAnnotations
    case TreeNodeInfo( _, nested, annotations ) => 
      annotations ++ ( nested flatMap { case (info, row) => collectAnnotationsRecursively( info, row ) } )
  }
  
  val ALPHANUM_COMPARATOR = new AlphanumComparator()

  val SI_PREFIXES_ORDERING_VALUES = Seq(
    ( "yotta", "(0)" ),
    ( "zetta", "(1)" ),
    ( "exa", "(2)" ),
    ( "peta", "(3)" ),
    ( "tera", "(4)" ),
    ( "giga", "(5)" ),
    ( "mega", "(6)" ),
    ( "kilo", "(7)" ),
    ( "hecto", "(8)" ),
    ( "deca", "(9)" ),

    ( "deci", "{0}" ),
    ( "centi", "{1}" ),
    ( "milli", "{2}" ),
    ( "micro", "{3}" ),
    ( "nano", "{4}" ),
    ( "pico", "{5}" ),
    ( "femto", "{6}" ),
    ( "atto", "{7}" ),
    ( "zepto", "{8}" ),
    ( "yocto", "{9}" ),

    ( "yobi", "{1}" ),
    ( "zebi", "{2}" ),
    ( "exbi", "{3}" ),
    ( "pebi", "{4}" ),
    ( "tebi", "{5}" ),
    ( "gibi", "{6}" ),
    ( "mebi", "{7}" ),
    ( "kibi", "{8}" ) )

  def expandSIPrefixes(
    s: String,
    replacements: Seq[( String, String )] = SI_PREFIXES_ORDERING_VALUES ): String =
    ( s /: replacements ) { case ( si, ( p, r ) ) => si.replaceAllLiterally( p, r ) }

}