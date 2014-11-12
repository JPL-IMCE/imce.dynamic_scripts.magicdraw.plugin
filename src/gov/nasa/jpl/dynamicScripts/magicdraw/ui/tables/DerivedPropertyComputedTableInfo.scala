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

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedTableInfo( e: Element,
                                             ek: MagicDrawElementKindDesignation,
                                             computedDerivedTable: ComputedDerivedTable )
  extends DerivedPropertyComputedInfo( e, ek, computedDerivedTable ) {

  require( computedDerivedTable.columnValueTypes.isDefined, s"A DerivedPropertyComputedTable must have explicitly-specified column value types!" )
  require( computedDerivedTable.columnValueTypes.get.nonEmpty, s"A DerivedPropertyComputedTable must have at least 1 column value type!" )
  
  val columnValueTypes = computedDerivedTable.columnValueTypes.get
  
  var values: Seq[Map[String, Seq[AbstractTreeNodeInfo]]] = null

  override def dispose: Unit = values = null

  override def getLabel: String = s"/${computedDerivedTable.name.hname}"
  
  override def getColumnCount: Int = columnValueTypes.size

  override def getColumnName( columnIndex: Int ): String = {
    require( 0 <= columnIndex && columnIndex < columnValueTypes.size )
    columnValueTypes( columnIndex ).typeName.hname
  }

  override def getRowCount: Int = if ( null == values ) 0 else values.size

  override def getValueAt( rowIndex: Int, columnIndex: Int ): Object = {
    require( 0 <= columnIndex && columnIndex < columnValueTypes.size )
    require( null != values )
    require( 0 <= rowIndex && rowIndex < values.size )
    val row = values( rowIndex )
    val column = columnValueTypes( columnIndex ).key.sname
    require( row.contains( column ) )
    row( column )
  }

  override def update: Seq[ValidationAnnotation] =
    if ( null != values ) {
      Seq()
    }
    else {
      val previousTime = System.currentTimeMillis()
      try {
        val message = computedDerivedTable.prettyPrint( "" ) + "\n"
        ClassLoaderHelper.createDynamicScriptClassLoader( computedDerivedTable ) match {
          case Failure( t ) =>
            ClassLoaderHelper.reportError( computedDerivedTable, message, t )
            return Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedTable,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedTable],
                classOf[MagicDrawElementKindDesignation], classOf[Element] ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedTable, message, t )
                    return Seq()

                  case Success( cm ) =>
                    val result = ClassLoaderHelper.invoke(
                      previousTime, Project.getProject( e ), null, cm, ek, e )
                    result match {
                      case Failure( t ) =>
                        ClassLoaderHelper.reportError( computedDerivedTable, message, t )
                        return Seq()
                      case Success( rs: Seq[_] ) =>
                        DerivedPropertyComputedTableInfo.asSeqOfMapOfStringToSeqOfAbstractTreeNodeInfo( rs ) match {
                          case None =>
                            ClassLoaderHelper.reportError( computedDerivedTable, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[Map[String, Seq[AbstractTreeNodeInfo]]], got: ${rs.getClass.getName}" ) )
                            return Seq()
                          case Some( rTable ) =>
                            values = rTable
                            values flatMap ( _.values flatMap ( _ flatMap ( _.getAnnotations ) ) )
                        }
                      case Success( x ) =>
                        ClassLoaderHelper.reportError( computedDerivedTable, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[Map[String, Seq[AbstractTreeNodeInfo]]], got: ${x.getClass.getName}" ) )
                        return Seq()
                    }
                }
            }
            finally {
              Thread.currentThread().setContextClassLoader( localClassLoader )
            }
          }
        }
      }
      finally {
        val currentTime = System.currentTimeMillis()
      }
    }

}

object DerivedPropertyComputedTableInfo {

  /**
   * Seq[Map[String, Seq[AbstractTreeNodeInfo]]]
   */
  def asSeqOfMapOfStringToSeqOfAbstractTreeNodeInfo( rs: Seq[_] ): Option[Seq[Map[String, Seq[AbstractTreeNodeInfo]]]] =
    if ( rs.forall( _ match {
      case m: Map[_, _] =>
        m.forall( _ match {
          case ( _: String, s: Seq[_] ) =>
            s.forall( _ match {
              case _: AbstractTreeNodeInfo => true
              case _                       => false
            } )
            true
          case ( _, _ ) =>
            false
        } )
        true
      case _ =>
        false
    } ) )
      Some( rs.asInstanceOf[Seq[Map[String, Seq[AbstractTreeNodeInfo]]]] )
    else None
}
