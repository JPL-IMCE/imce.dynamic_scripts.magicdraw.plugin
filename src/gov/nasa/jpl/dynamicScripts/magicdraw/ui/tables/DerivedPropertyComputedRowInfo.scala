package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.awt.event.ActionEvent
import scala.util.Failure
import scala.util.Success
import com.nomagic.magicdraw.annotation.Annotation
import com.nomagic.magicdraw.core.Project
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedDerivedFeature
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AbstractTreeNodeInfo
import com.nomagic.magicdraw.core.Project
import java.awt.event.ActionEvent
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedRowInfo[E <: Element]( metaclass: Class[_],
                                                    e: E,
                                                    c: Classifier,
                                                    computedDerivedFeature: ComputedDerivedFeature )
  extends DerivedPropertyComputedInfo( metaclass, e, c, computedDerivedFeature ) {

  var values: Seq[AbstractTreeNodeInfo] = null

  override def dispose: Unit = values = null

  override def getColumnCount: Int = 1

  override def getColumnName( columnIndex: Int ): String = {
    require( columnIndex != 0 )
    getLabel
  }

  override def getRowCount: Int = if ( null == values ) 0 else values.size

  override def getValueAt( rowIndex: Int, columnIndex: Int ): Object = {
    require( columnIndex != 0 )
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
                      case Success( x ) =>
                        DerivedPropertyComputedInfo.anyToInfo( x ) match {
                          case Failure( t ) =>
                            ClassLoaderHelper.reportError( computedDerivedFeature, message, t )
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
