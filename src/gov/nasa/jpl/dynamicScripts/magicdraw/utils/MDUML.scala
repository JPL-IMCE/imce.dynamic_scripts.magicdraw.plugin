/**
 * License Terms
 *
 * Copyright (c) 2014, California
 * Institute of Technology ("Caltech").  U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *
 *  *   Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *  *   Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the
 *      distribution.
 *
 *  *   Neither the name of Caltech nor its operating division, the Jet
 *      Propulsion Laboratory, nor the names of its contributors may be
 *      used to endorse or promote products derived from this software
 *      without specific prior written permission.
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
package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageImport
import scala.collection.JavaConversions._
import scala.language.implicitConversions
import scala.language.postfixOps
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object MDUML {

  def elementPackageContainmentIterator( e: Element ): Iterator[Package] = new Iterator[Package] {
    private var p = ModelHelper.findParentOfType( e, classOf[Package] )

    def hasNext: Boolean = ( p != null )

    def next: Package = {
      val result = p
      p = p.getNestingPackage()
      result
    }
  }

  def getAllImportedPackages( p: Package ): Set[Package] = collectImportedPackages( p.getPackageImport().toList, Set() )

  def collectImportedPackages( imports: List[PackageImport], imported: Set[Package] ): Set[Package] =
    imports match {
      case Nil => imported
      case x :: xs =>
        val ip = x.getImportedPackage()
        if ( imported.contains( ip ) ) collectImportedPackages( xs, imported )
        else collectImportedPackages( ip.getPackageImport().toList ++ xs, imported + ip )
    }

  def getAllGeneralClassifiersIncludingSelf( cls: Classifier ): List[Classifier] = {

    def getAllGeneralClassifiers( cls: Classifier ): List[Classifier] = {
      val generalClassifiers: java.util.List[Classifier] = new java.util.ArrayList[Classifier]()
      ModelHelper.collectGeneralClassifiersRecursivelly( generalClassifiers, cls )
      generalClassifiers.toList
    }

    List( cls ) ++ getAllGeneralClassifiers( cls )
  }

}