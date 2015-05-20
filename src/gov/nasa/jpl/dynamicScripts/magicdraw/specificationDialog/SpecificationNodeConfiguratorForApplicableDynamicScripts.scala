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
package gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.immutable.SortedSet
import scala.language.implicitConversions
import scala.language.postfixOps
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.dialogs.specifications.configurator.ISpecificationNodeConfigurator
import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.ConfigurableNodeFactory
import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.IConfigurableNode
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BinaryDerivationRefresh.DELAYED_COMPUTATION_UNTIL_INVOKED
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BinaryDerivationRefresh.EAGER_COMPUTATION_AS_NEEDED
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedDerivedFeature
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsProjectListener
import gov.nasa.jpl.dynamicScripts.magicdraw.designations.MagicDrawElementKindDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables._
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class SpecificationNodeConfiguratorForApplicableDynamicScripts extends ISpecificationNodeConfigurator {

  override def configure( node: IConfigurableNode, e: Element ): Unit = {
    val log = MDGUILogHelper.getMDPluginsLog

    val previousTime = System.currentTimeMillis()
    try {
      val characterizationContext: DynamicScriptsProjectListener =
        DynamicScriptsPlugin.getInstance().getCharacterizationContext()
      log.info( s"${this.getClass().getName()}: configure specification dialog for ${e.getHumanType()}}" )

      val project = Project.getProject( e )
      if ( project == null )
        return

      val p = DynamicScriptsPlugin.getInstance
      val mName = ClassTypes.getShortName( e.getClassType )
      val es = StereotypesHelper.getStereotypes( e ).toList

      val mDerivedFeatures = p.getRelevantMetaclassComputedCharacterizations( mName )
      val sDerivedFeatures = StereotypesHelper.getStereotypesHierarchy( e ) flatMap ( s =>
        Option.apply( s.getProfile ) match {
          case Some( pf ) =>
            p.getRelevantStereotypeComputedCharacterizations(
              mName, pf.getQualifiedName(), s.getQualifiedName() )
          case None =>
            Map[String, SortedSet[DynamicScriptsTypes.ComputedCharacterization]]()
        } )
      val cDerivedFeatures = p.getRelevantClassifierComputedCharacterizations( e )
      val csDerivedFeatures = p.getRelevantStereotypedClassifierComputedCharacterizations( e )

      val allDerivedFeatures = mDerivedFeatures ++ sDerivedFeatures ++ cDerivedFeatures ++ csDerivedFeatures
      addDerivedFeatures( project, node, allDerivedFeatures, e )

    } finally {
      val currentTime = System.currentTimeMillis()
      log.info( s"SpecificationNodeConfiguratorForApplicableDynamicScripts.configure took ${currentTime - previousTime} ms" )
    }
  }

  def addDerivedFeatures(
    project: Project,
    node: IConfigurableNode,
    allDerivedFeatures: Map[String, SortedSet[DynamicScriptsTypes.ComputedCharacterization]],
    e: Element ): Unit = {

    type DerivedHierarchicalTable = DerivedPropertiesHierarchicalTableModel[AbstractDisposableTableModel]

    val entries = allDerivedFeatures.keys.toList sorted;
    entries foreach { entry =>
      val characterizations = allDerivedFeatures( entry )
      characterizations foreach {
        case cs if ( cs.computedDerivedFeatures nonEmpty ) =>

          val ek = MagicDrawElementKindDesignation.getMagicDrawDesignation( project, cs.characterizesInstancesOf )

          val early = cs.computedDerivedFeatures filter ( EAGER_COMPUTATION_AS_NEEDED == _.refresh ) flatMap
            ( computedFeatureToInfo( cs, e, ek, _ ) )

          val delayed = cs.computedDerivedFeatures filter ( DELAYED_COMPUTATION_UNTIL_INVOKED == _.refresh ) flatMap
            ( computedFeatureToInfo( cs, e, ek, _ ) )

          val earlyNode = ConfigurableNodeFactory.createConfigurableNode( SpecificationComputedNode[DerivedHierarchicalTable](
            ID = s"${entry} EARLY",
            label = s"${cs.name.hname} (derivations computed early)",
            e,
            table = new DerivedHierarchicalTable( e, early ) ) )

          node.insertNode( IConfigurableNode.DOCUMENTATION_HYPERLINKS, IConfigurableNode.Position.BEFORE, earlyNode )

          if ( delayed nonEmpty ) {

            val delayedNode = ConfigurableNodeFactory.createConfigurableNode(
              SpecificationComputedNode[DerivedHierarchicalTable](
                ID = s"${entry} DELAYED",
                label = s"${cs.name.hname} (derivations computed on-demand)",
                e,
                table = new DerivedHierarchicalTable( e, delayed ) ) )

            earlyNode.addNode( delayedNode )

          }
      }
    }
  }

  def computedFeatureToInfo(
    cs: DynamicScriptsTypes.ComputedCharacterization,
    e: Element,
    ek: MagicDrawElementKindDesignation,
    computedDerivedFeature: ComputedDerivedFeature ): Option[AbstractDisposableTableModel] =
    try {
      Some( computedDerivedFeature match {
        case f: DynamicScriptsTypes.ComputedDerivedProperty =>
          DerivedPropertyComputedRowInfo( cs, e, ek, f )
        case f: DynamicScriptsTypes.ComputedDerivedTable =>
          DerivedPropertyComputedTableInfo( cs, e, ek, f )
        case f: DynamicScriptsTypes.ComputedDerivedTree =>
          DerivedPropertyComputedTreeInfo( cs, e, ek, f )
        case f: DynamicScriptsTypes.ComputedDerivedWidget =>
          DerivedPropertyComputedWidget( cs, e, ek, f )
      } )
    } catch {
      case t: Throwable =>
        val log = MDGUILogHelper.getMDPluginsLog
        log.error(s"Cannot create computed derived feature for '${computedDerivedFeature}' because of an error", t)
        None
    }

}