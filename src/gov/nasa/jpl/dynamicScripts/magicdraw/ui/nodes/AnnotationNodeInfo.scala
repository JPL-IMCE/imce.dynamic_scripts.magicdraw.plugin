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
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import java.lang.IllegalArgumentException

import javax.swing.JOptionPane
import com.nomagic.magicdraw.annotation.Annotation
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

import scala.collection.immutable._
import scala.{Boolean, Int}
import scala.Predef.String

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class AnnotationNodeInfo(
  override val identifier: String,
  val a: ValidationAnnotation )
  extends AbstractTreeNodeInfo( identifier ) {

  if ( a == null )
    throw new IllegalArgumentException( "Annotation should not be null!" )

  if ( a.annotation.getSeverity == null )
    throw new IllegalArgumentException( "Annotation.severity should not be null!" )

  if ( a.annotation.getKind == null )
    throw new IllegalArgumentException( "Annotation.kind should not be null!" )

  val compareKey: String = identifier + a.annotation.getTarget.getHumanType + a.annotation.getTarget.getID

  def getAnnotation: Annotation = a.annotation
  def getAnnotationKind: String = a.annotation.getKind

  def getAnnotationMessageKind: Int = a.annotation.getSeverity.getName match {
    case Annotation.ERROR   => JOptionPane.ERROR_MESSAGE
    case Annotation.WARNING => JOptionPane.WARNING_MESSAGE
    case Annotation.INFO    => JOptionPane.INFORMATION_MESSAGE
    case _                  => JOptionPane.PLAIN_MESSAGE
  }

  def isError: Boolean = a.annotation.getSeverity.getName == Annotation.ERROR
  def isWarning: Boolean = a.annotation.getSeverity.getName == Annotation.WARNING
  def isInfo: Boolean = a.annotation.getSeverity.getName == Annotation.INFO
  
  def getAnnotations = Seq(a)
}