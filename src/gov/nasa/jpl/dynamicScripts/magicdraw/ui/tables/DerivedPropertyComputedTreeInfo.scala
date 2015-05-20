/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.awt.event.ActionEvent
import scala.language.existentials
import scala.util.Failure
import scala.util.Success
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes._
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation
import com.jidesoft.grid.Row
import com.jidesoft.grid.TreeTableModel
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.TreeNodeInfo
import com.nomagic.utils.Utilities
import com.nomagic.magicdraw.uml.ClassTypes

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedTreeInfo(
  cs: ComputedCharacterization,
  e: Element,
  ek: MagicDrawElementKindDesignation,
  computedDerivedTree: ComputedDerivedTree )
  extends TreeTableModel[DerivedPropertyComputedTreeRow]
  with AbstractDisposableTableModel {

  require( computedDerivedTree.columnValueTypes.isDefined, s"A DerivedPropertyComputedTree must have explicitly-specified column value types!" )
  require( computedDerivedTree.columnValueTypes.get.nonEmpty, s"A DerivedPropertyComputedTree must have at least 1 column value type!" )

  val derivedElementClassType = {
    val classTypeName = cs.characterizesInstancesOf.metaclass.sname
    val classTypeDef = ClassTypes.getClassType( classTypeName )
    require( classTypeDef != null, "No MagicDraw ClassType available for ComputedDerivedTree className: '"+classTypeName+"'" )
    classTypeDef
  }

  val columnValueTypes = computedDerivedTree.columnValueTypes.get

  var values: Seq[( AbstractTreeNodeInfo, Map[String, AbstractTreeNodeInfo] )] = null

  val treeRows = scala.collection.mutable.HashMap[AbstractTreeNodeInfo, DerivedPropertyComputedTreeRow]()

  val defaultLabel: String = s"/${computedDerivedTree.name.hname}"
  var label: String = defaultLabel

  override def dispose: Unit = {
    values = null
    treeRows.clear
  }

  override def getLabel: String = label

  override def getColumnClass( columnIndex: Int ): Class[_] =
    if ( 0 == columnIndex )
      classOf[DerivedPropertyComputedTreeRow]
    else {
      require( 0 < columnIndex && columnIndex <= columnValueTypes.size )
      classOf[Object]
    }

  override def getColumnCount: Int = 1 + columnValueTypes.size

  override def getColumnName( columnIndex: Int ): String =
    if ( 0 == columnIndex )
      defaultLabel
    else {
      require( 0 < columnIndex && columnIndex <= columnValueTypes.size )
      columnValueTypes( columnIndex - 1 ).typeName.hname
    }

  protected def makeTreeRow( tree: TreeNodeInfo, row: Map[String, AbstractTreeNodeInfo] ): DerivedPropertyComputedTreeRow = {
    val children = tree.nested map {
      case ( _info: AbstractTreeNodeInfo, _row: Map[String, AbstractTreeNodeInfo] ) =>
        _info match {
          case _tree: TreeNodeInfo =>
            makeTreeRow( _tree, _row )

          case _node =>
            val treeRow = DerivedPropertyComputedTreeRow( _node, _row, Seq(), computedDerivedTree )
            treeRows += ( _node -> treeRow )
            treeRow
        }
    }
    val treeRow = DerivedPropertyComputedTreeRow( tree, row, children, computedDerivedTree )
    treeRows += ( tree -> treeRow )
    treeRow
  }

  protected def addChildRows( parent: DerivedPropertyComputedTreeRow ): Unit =
    parent.children foreach { child =>
      addRow( parent, child )
      addChildRows( child )
    }

  protected def addValue( info: AbstractTreeNodeInfo, row: Map[String, AbstractTreeNodeInfo] ): Unit = info match {
    case tree: TreeNodeInfo =>
      val treeRow = makeTreeRow( tree, row )
      addRow( treeRow )
      addChildRows( treeRow )

    case node =>
      val nodeRow = DerivedPropertyComputedTreeRow( node, row, Seq(), computedDerivedTree )
      treeRows += ( node -> nodeRow )
      addRow( nodeRow )
  }

  override def update: Seq[ValidationAnnotation] =
    if ( null != values ) {
      Seq()
    } else {
      val previousTime = System.currentTimeMillis()
      try {
        val message = computedDerivedTree.prettyPrint( "" )+"\n"
        ClassLoaderHelper.createDynamicScriptClassLoader( computedDerivedTree ) match {
          case Failure( t ) =>
            ClassLoaderHelper.reportError( computedDerivedTree, message, t )
            return Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedTree,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedTree],
                classOf[MagicDrawElementKindDesignation], derivedElementClassType ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedTree, message, t )
                    return Seq()

                  case Success( cm ) =>
                    val result = ClassLoaderHelper.invokeAndReport(
                      previousTime, Project.getProject( e ), null, cm, ek, e )
                    result match {
                      case Failure( t ) =>
                        return Seq()
                      case Success( rs: Seq[_] ) =>
                        DerivedPropertyComputedTreeInfo.asSeqOfMapOfStringToAbstractTreeNodeInfo( rs ) match {
                          case None =>
                            ClassLoaderHelper.reportError( computedDerivedTree, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[( AbstractTreeNodeInfo, Map[String, AbstractTreeNodeInfo] )], got: ${rs.getClass.getName}" ) )
                            return Seq()
                          case Some( rTable ) =>
                            val annotations = scala.collection.mutable.Buffer[ValidationAnnotation]()
                            Utilities.invokeAndWaitOnDispatcher( new Runnable() {
                              override def run: Unit = {
                                values = rTable
                                treeRows.clear
                                label = s"${defaultLabel} => ${values.size} values"
                                values foreach {
                                  case ( info, row ) =>
                                    addValue( info, row )
                                    annotations ++= AbstractTreeNodeInfo.collectAnnotationsRecursively( info, row )
                                }
                              }
                            } )
                            annotations.toSeq

                        }
                      case Success( x ) =>
                        ClassLoaderHelper.reportError( computedDerivedTree, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[Map[String, Seq[AbstractTreeNodeInfo]]], got: ${x.getClass.getName}" ) )
                        return Seq()
                    }
                }
            } finally {
              Thread.currentThread().setContextClassLoader( localClassLoader )
            }
          }
        }
      } finally {
        val currentTime = System.currentTimeMillis()
      }
    }

}

object DerivedPropertyComputedTreeInfo {

  /**
   * Seq[Map[String, AbstractTreeNodeInfo]]
   */
  def asSeqOfMapOfStringToAbstractTreeNodeInfo( rs: Seq[_] ): Option[Seq[( AbstractTreeNodeInfo, Map[String, AbstractTreeNodeInfo] )]] =
    if ( rs.forall( _ match {
      case ( info: AbstractTreeNodeInfo, m: Map[_, _] ) =>
        m.forall( _ match {
          case ( _: String, _: AbstractTreeNodeInfo ) => true
          case ( _, _ )                               => false
        } )
      case _ =>
        false
    } ) )
      Some( rs.asInstanceOf[Seq[( AbstractTreeNodeInfo, Map[String, AbstractTreeNodeInfo] )]] )
    else None
}