package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.uml.UUIDRegistry
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedDerivedFeature
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.LabelNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.ReferenceNodeInfo

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
abstract class DerivedPropertyComputedInfo[E <: Element]( metaclass: Class[_],
                                                          e: E,
                                                          c: Classifier,
                                                          computedDerivedFeature: ComputedDerivedFeature )
  extends AbstractDisposableTableModel(
    table = new java.util.Vector[java.util.Vector[String]](),
    columns = new java.util.Vector[String]() ) {

  override def getLabel: String = s"/${computedDerivedFeature.name}"
  
  override def isCellEditable(row: Int, column: Int): Boolean = false
  
  protected def getAnnotationSummary(annotations: Set[Annotation]): String = 
    if (annotations.isEmpty) "no annotations"
    else {
      val counts = annotations.map (_.getSeverity.getName) groupBy (w=>w) mapValues (_.size)
      val summary = counts.keys.toList.sorted map { w => s"${counts.get(w).get} $w(s)" } mkString ("; ")
      summary
    }

}

object DerivedPropertyComputedInfo {
  
  /**
   * @param x One of the following:
   * - `Failure(_)` if there is no recognized mapping for `x`
   * - `Success(_)` A sequence of [[AbstractTreeNodeInfo]] obtained from one of the following mappings:
   *  -- [[InstanceSpecification]] maps to [[ReferenceNodeInfo]]
   *  -- [[Element]] maps to [[ReferenceNodeInfo]]
   *  -- [[AbstractTreeNodeInfo]] maps as-is
   *  -- [[Boolean]] maps to [[LabelNodeInfo]]
   *  -- [[Int]] maps to [[LabelNodeInfo]]
   *  -- [[String]] maps to [[LabelNodeInfo]]
   *  -- [[Traversable[_]]] maps `anyToInfo(_)` recursively; returning all sequence results merged or the first `Failure(_)` result obtained
   */
  def anyToInfo( x: Any ): Try[Seq[AbstractTreeNodeInfo]] = x match {
    case r: InstanceSpecification =>
      Success( Seq( ReferenceNodeInfo(
        r.getQualifiedName, r,
        r.getQualifiedName,
        s"${r.getHumanName} [UUID=${UUIDRegistry.getUUID( r )}]" ) ) )
    case r: Element =>
      Success( Seq( ReferenceNodeInfo(
        r.getHumanName, r,
        s"${r.getHumanName} [ID=${r.getID}]",
        s"${r.getHumanName} [UUID=${UUIDRegistry.getUUID( r )}]" ) ) )
    case r: AbstractTreeNodeInfo =>
      Success( Seq( r ) )
    case r: Int =>
      Success( Seq( LabelNodeInfo( r.toString ) ) )
    case r: Boolean =>
      Success( Seq( LabelNodeInfo( r.toString ) ) )
    case r: String =>
      Success( Seq( LabelNodeInfo( r ) ) )
    case r: Traversable[_] =>
      val f0: Seq[Failure[Seq[AbstractTreeNodeInfo]]] = Seq()
      val s0: Seq[AbstractTreeNodeInfo] = Seq()
      val ( fn, sn ) = ( ( f0, s0 ) /: r ) {
        case ( ( fi, si ), ri ) =>
          anyToInfo( ri ) match {
            case f: Failure[_] => ( f +: fi, si )
            case Success( s )  => ( fi, si ++ s )
          }
      }
      if (fn.nonEmpty) fn.head
      else Success( sn )
    case r =>
      Failure( new IllegalArgumentException( s"Unrecognized result: ${r}" ) )
  }
}