/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.lang.IllegalArgumentException

import scala.collection.immutable._
import scala.util.{Failure, Success, Try}
import scala.{Any, Boolean, Int, StringContext}
import scala.Predef.{require, String}

import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.uml.UUIDRegistry
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