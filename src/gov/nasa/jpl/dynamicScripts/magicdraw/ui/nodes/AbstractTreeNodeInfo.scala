/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2015, California Institute of Technology ("Caltech").
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

  def dispose(): Unit = {}

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