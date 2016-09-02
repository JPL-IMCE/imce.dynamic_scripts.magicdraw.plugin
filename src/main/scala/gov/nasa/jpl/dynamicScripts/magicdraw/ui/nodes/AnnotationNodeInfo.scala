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