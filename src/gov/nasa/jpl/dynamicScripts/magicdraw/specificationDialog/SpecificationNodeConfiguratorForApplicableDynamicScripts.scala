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
import scala.language.implicitConversions
import scala.language.postfixOps

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.dialogs.specifications.configurator.ISpecificationNodeConfigurator
import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.ConfigurableNodeFactory
import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.IConfigurableNode
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BinaryDerivationRefresh.DELAYED_COMPUTATION_UNTIL_INVOKED
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BinaryDerivationRefresh.EAGER_COMPUTATION_AS_NEEDED
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedDerivedFeature
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsProjectListener
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.DerivedPropertiesHierarchicalTableModel
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.DerivedPropertyComputedInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.DerivedPropertyComputedRowInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables.DerivedPropertyComputedTableInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class SpecificationNodeConfiguratorForApplicableDynamicScripts extends ISpecificationNodeConfigurator {

  override def configure( node: IConfigurableNode, e: Element ): Unit = {
    val log = MDGUILogHelper.getMDPluginsLog

    val previousTime = System.currentTimeMillis()
    try {
      val characterizationContext: DynamicScriptsProjectListener = DynamicScriptsPlugin.getInstance().getCharacterizationContext()
      log.info( s"${this.getClass().getName()}: configure specification dialog for ${e.getHumanType()}}" )

      val project = Project.getProject( e )
      if ( project == null )
        return

      val p = DynamicScriptsPlugin.getInstance
      val metaclass = e.getClassType
      val mName = ClassTypes.getShortName( metaclass )
      val es = StereotypesHelper.getStereotypes( e ).toList

      def dynamicScriptToolbarFilter( df: ComputedDerivedFeature ): Boolean = isComputedDerivedFeatureAvailable( e, es )( df )
      val mDerivedFeatures = p.getRelevantMetaclassComputedCharacterizations( mName, dynamicScriptToolbarFilter )
      val sDerivedFeatures = StereotypesHelper.getStereotypesHierarchy( e ) flatMap ( s => p.getRelevantStereotypeComputedCharacterizations( mName, s.getProfile().getQualifiedName(), s.getQualifiedName(), dynamicScriptToolbarFilter ) )
      val cDerivedFeatures = p.getRelevantClassifierComputedCharacterizations( e, dynamicScriptToolbarFilter )
      val csDerivedFeatures = p.getRelevantStereotypedClassifierComputedCharacterizations( e, dynamicScriptToolbarFilter )

      val allDerivedFeatures = mDerivedFeatures ++ sDerivedFeatures ++ cDerivedFeatures ++ csDerivedFeatures
      val earlyDerivedFeatures = allDerivedFeatures flatMap { 
        case (name, dfs) => 
        dfs filter (EAGER_COMPUTATION_AS_NEEDED == _.refresh) match {
          case Seq() => None
          case s => Some( name -> s )
        }        
      }
      val delayedDerivedFeatures = allDerivedFeatures flatMap { 
        case (name, dfs) => 
        dfs filter (DELAYED_COMPUTATION_UNTIL_INVOKED == _.refresh) match {
          case Seq() => None
          case s => Some( name -> s )
        }        
      }
      val entries = (earlyDerivedFeatures.keys.toSet ++ delayedDerivedFeatures.keys.toSet).toSeq sorted;
      
      type DerivedPropertyInfo = DerivedPropertyComputedInfo[Element]
      type DerivedHierarchicalTable = DerivedPropertiesHierarchicalTableModel[DerivedPropertyInfo, Element]
      
      entries foreach { entry =>
        val early = earlyDerivedFeatures.get(entry)
        val delayed = delayedDerivedFeatures.get(entry)
        (early, delayed) match {
          case ( Some(efs), Some(dfs) ) =>
            val eps = efs map { ef => computedFeatureToInfo( metaclass, e, c=null, computedDerivedFeature=ef ) }
            val dps = dfs map { df => computedFeatureToInfo( metaclass, e, c=null, computedDerivedFeature=df ) }
            
            val esNode = ConfigurableNodeFactory.createConfigurableNode( SpecificationComputedNode[DerivedHierarchicalTable, Element](
               ID=s"${entry} (early derived features)", 
               label=s"${e.getHumanType}",
               e,
               table=new DerivedHierarchicalTable(e, eps) ) )
            
            val dsNode = ConfigurableNodeFactory.createConfigurableNode( SpecificationComputedNode[DerivedHierarchicalTable, Element](
               ID=s"${entry} (late derived features)", 
               label=s"${e.getHumanType}",
               e,
               table=new DerivedHierarchicalTable(e, dps) ) )
              
            node.insertNode( IConfigurableNode.DOCUMENTATION_HYPERLINKS, IConfigurableNode.Position.BEFORE, esNode )
            esNode.addNode( dsNode )
            
          case ( Some(efs), None ) =>
            val eps = efs map { ef => computedFeatureToInfo( metaclass, e, c=null, computedDerivedFeature=ef ) }
            
            val esNode = ConfigurableNodeFactory.createConfigurableNode( SpecificationComputedNode[DerivedHierarchicalTable, Element](
               ID=s"${entry} (early derived features)", 
               label=s"${e.getHumanType}",
               e,
               table=new DerivedHierarchicalTable(e, eps) ) )            
              
            node.insertNode( IConfigurableNode.DOCUMENTATION_HYPERLINKS, IConfigurableNode.Position.BEFORE, esNode )
            
          case ( None, Some(dfs) ) =>
            val dps = dfs map { df => computedFeatureToInfo( metaclass, e, c=null, computedDerivedFeature=df ) }
            
            val dsNode = ConfigurableNodeFactory.createConfigurableNode( SpecificationComputedNode[DerivedHierarchicalTable, Element](
               ID=s"${entry} (late derived features)", 
               label=s"${e.getHumanType}",
               e,
               table=new DerivedHierarchicalTable(e, dps) ) )
              
            node.insertNode( IConfigurableNode.DOCUMENTATION_HYPERLINKS, IConfigurableNode.Position.BEFORE, dsNode )
            
          case ( None, None ) =>
            ()
        }
      }
    }
    finally {
      val currentTime = System.currentTimeMillis()
      log.info( s"SpecificationNodeConfiguratorForApplicableDynamicScripts.configure took ${currentTime - previousTime} ms" )
    }
  }

  def isComputedDerivedFeatureAvailable( e: Element, es: List[Stereotype] )( df: ComputedDerivedFeature ): Boolean = ???

  def computedFeatureToInfo[E <: Element]( 
      metaclass: Class[_],
      e: E,
      c: Classifier,
      computedDerivedFeature: ComputedDerivedFeature ): 
  DerivedPropertyComputedInfo[E] = computedDerivedFeature match {
    case p: DynamicScriptsTypes.ComputedDerivedProperty =>
      DerivedPropertyComputedRowInfo[E]( metaclass, e, c, p )
    case t: DynamicScriptsTypes.ComputedDerivedTable =>
      DerivedPropertyComputedTableInfo[E]( metaclass, e, c, t, columnLabels = Seq() )
  }
}