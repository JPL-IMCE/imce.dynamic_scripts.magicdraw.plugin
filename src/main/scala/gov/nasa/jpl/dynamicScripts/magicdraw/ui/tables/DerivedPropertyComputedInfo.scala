/*
 * Copyright 2014 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * License Terms
 */

package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.lang.IllegalArgumentException

import scala.collection.immutable._
import scala.util.{Failure, Success, Try}
import scala.{Any, Boolean, Int, StringContext}
import scala.Predef.{require, String}

import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedDerivedFeature
import gov.nasa.jpl.dynamicScripts.magicdraw.designations.MagicDrawElementKindDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.LabelNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.ReferenceNodeInfo

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
abstract class DerivedPropertyComputedInfo
( e: Element,
  ek: MagicDrawElementKindDesignation,
  computedDerivedFeature: ComputedDerivedFeature )
  extends AbstractDefaultDisposableTableModel(
    table = new java.util.Vector[java.util.Vector[String]](),
    columns = new java.util.Vector[String]() ) {

  def getProject: Project = {
    val p = Project.getProject(e)
    require(null != p)
    p
  }

  override def isCellEditable( row: Int, column: Int ): Boolean = false

  protected def getAnnotationSummary( annotations: Set[Annotation] ): String =
    if ( annotations.isEmpty ) "no annotations"
    else {
      val counts = annotations.map( _.getSeverity.getName ) groupBy ( w => w ) mapValues ( _.size )
      val summary = counts.keys.toList.sorted map { w => s"${counts.get( w ).get} $w(s)" } mkString ( "; " )
      summary
    }

}

object DerivedPropertyComputedInfo {

  /**
    * @param x One of the following:
    * - `Failure(_)` if there is no recognized mapping for `x`
    * - `Success(_)` A sequence of [[gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo]]
    *   obtained from one of the following mappings:
    *   -- `InstanceSpecification` maps to [[gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.ReferenceNodeInfo]]
    *   -- `Element` maps to [[gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.ReferenceNodeInfo]]
    *   -- [[gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo]] maps as-is
    *   -- `java.lang.Boolean` maps to [[gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.LabelNodeInfo]]
    *   -- `java.lang.Integer` maps to [[gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.LabelNodeInfo]]
    *   -- `java.lang.String` maps to [[gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.LabelNodeInfo]]
    *   -- `Traversable[_]` maps `anyToInfo(_)` recursively; returning
    *      all sequence results merged or the first `Failure(_)` result obtained
    */
  def anyToInfo( x: Any ): Try[Seq[AbstractTreeNodeInfo]] = x match {
    case r: InstanceSpecification =>
      Success( Seq( ReferenceNodeInfo( r.getQualifiedName, r ) ) )
    case r: Element =>
      Success( Seq( ReferenceNodeInfo( r.getHumanName, r ) ) )
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
      if ( fn.nonEmpty ) fn.head
      else Success( sn )
    case r =>
      Failure( new IllegalArgumentException( s"Unrecognized result: $r" ) )
  }
}