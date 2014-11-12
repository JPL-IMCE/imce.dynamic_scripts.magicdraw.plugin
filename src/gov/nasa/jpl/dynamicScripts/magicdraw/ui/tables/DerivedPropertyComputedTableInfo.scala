package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import scala.language.existentials
import java.awt.event.ActionEvent
import scala.util.Failure
import scala.util.Success
import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedDerivedFeature
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import com.nomagic.magicdraw.core.Project
import java.awt.event.ActionEvent
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedTableInfo[E <: Element]( metaclass: Class[_],
                                                      e: E,
                                                      c: Classifier,
                                                      computedDerivedFeature: ComputedDerivedFeature,
                                                      columnLabels: Seq[( String, String )] )
  extends DerivedPropertyComputedInfo( metaclass, e, c, computedDerivedFeature ) {

  var values: Seq[Map[String, Seq[AbstractTreeNodeInfo]]] = null

  override def dispose: Unit = values = null

  override def getColumnCount: Int = columnLabels.size

  override def getColumnName( columnIndex: Int ): String = {
    require( 0 <= columnIndex && columnIndex < columnLabels.size )
    columnLabels( columnIndex )._2
  }

  override def getRowCount: Int = if ( null == values ) 0 else values.size

  override def getValueAt( rowIndex: Int, columnIndex: Int ): Object = {
    require( 0 <= columnIndex && columnIndex < columnLabels.size )
    require( null != values )
    require( 0 <= rowIndex && rowIndex < values.size )
    val row = values( rowIndex )
    val column = columnLabels( columnIndex )._1
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
        val message = computedDerivedFeature.prettyPrint( "" ) + "\n"
        ClassLoaderHelper.createDynamicScriptClassLoader( computedDerivedFeature ) match {
          case Failure( t ) =>
            ClassLoaderHelper.reportError( computedDerivedFeature, message, t )
            return Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedFeature,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedFeature],
                classOf[Class[_]], classOf[Element], classOf[Classifier] ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedFeature, message, t )
                    return Seq()

                  case Success( cm ) =>
                    val result = ClassLoaderHelper.invoke(
                      previousTime, Project.getProject( e ), null, cm,
                      computedDerivedFeature, metaclass, e, c )
                    result match {
                      case Failure( t ) =>
                        ClassLoaderHelper.reportError( computedDerivedFeature, message, t )
                        return Seq()
                      case Success( rs: Seq[_] ) =>
                        DerivedPropertyComputedTableInfo.asSeqOfMapOfStringToSeqOfAbstractTreeNodeInfo( rs ) match {
                          case None =>
                            ClassLoaderHelper.reportError( computedDerivedFeature, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[Map[String, Seq[AbstractTreeNodeInfo]]], got: ${rs.getClass.getName}" ) )
                            return Seq()
                          case Some( rTable ) =>
                            values = rTable
                            values flatMap ( _.values flatMap ( _ flatMap ( _.getAnnotations ) ) )
                        }
                      case Success( x ) =>
                        ClassLoaderHelper.reportError( computedDerivedFeature, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[Map[String, Seq[AbstractTreeNodeInfo]]], got: ${x.getClass.getName}" ) )
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
