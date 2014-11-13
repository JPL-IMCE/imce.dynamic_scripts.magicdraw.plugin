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
case class DerivedPropertyComputedRowInfo( e: Element,
                                           ek: MagicDrawElementKindDesignation,
                                           computedDerivedProperty: ComputedDerivedProperty )
  extends DerivedPropertyComputedInfo( e, ek, computedDerivedProperty ) {

  var values: Seq[AbstractTreeNodeInfo] = null

  override def dispose: Unit = values = null

  override def getLabel: String =
    computedDerivedProperty.valueType match {
    case None => s"/${computedDerivedProperty.name.hname}"
    case Some( vt ) => vt.typeName.hname
  }
  
  override def getColumnCount: Int = 1

  override def getColumnName( columnIndex: Int ): String = {
    require( columnIndex == 0 )
    getLabel
  }

  override def getRowCount: Int = if ( null == values ) 0 else values.size

  override def getValueAt( rowIndex: Int, columnIndex: Int ): Object = {
    require( columnIndex == 0 )
    require( null != values )
    require( 0 <= rowIndex && rowIndex < values.size )
    values( rowIndex )
  }

  override def update: Seq[ValidationAnnotation] =
    if ( null != values ) {
      Seq()
    }
    else {
      val previousTime = System.currentTimeMillis()
      try {
        val message = computedDerivedProperty.prettyPrint( "" ) + "\n"
        ClassLoaderHelper.createDynamicScriptClassLoader( computedDerivedProperty ) match {
          case Failure( t ) =>
            ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
            return Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedProperty,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedProperty],
                classOf[MagicDrawElementKindDesignation], classOf[Element] ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
                    return Seq()

                  case Success( cm ) =>
                    val result = ClassLoaderHelper.invokeAndReport(
                      previousTime, Project.getProject( e ), null, cm, ek, e )
                    result match {
                      case Failure( t ) =>
                        return Seq()
                      case Success( x ) =>
                        DerivedPropertyComputedInfo.anyToInfo( x ) match {
                          case Failure( t ) =>
                            ClassLoaderHelper.reportError( computedDerivedProperty, message, t )
                            return Seq()
                          case Success( nodes ) =>
                            values = nodes
                            values flatMap ( _.getAnnotations )
                        }
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
