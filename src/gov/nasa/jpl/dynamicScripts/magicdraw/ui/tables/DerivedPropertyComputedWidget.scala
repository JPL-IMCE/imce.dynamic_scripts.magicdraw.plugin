/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.awt.Component
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

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class DerivedPropertyComputedWidget(
  cs: ComputedCharacterization, e: Element,
  ek: MagicDrawElementKindDesignation,
  computedDerivedWidget: ComputedDerivedWidget )
  extends DerivedPropertyComputedInfo( e, ek, computedDerivedWidget ) {

  var value: Component = null

  val defaultLabel: String = s"/${computedDerivedWidget.name.hname}"
  var label: String = defaultLabel

  override def dispose: Unit = {
    value = null
  }

  override def getLabel: String = label

  override def getColumnClass( columnIndex: Int ): Class[_] =
    classOf[Object]

  override def getColumnCount: Int = 1

  override def getColumnName( columnIndex: Int ): String =
    defaultLabel

  override def getRowCount: Int =
    if ( null == value ) 0
    else 1

  def getComponent: Component = value

  override def getValueAt( rowIndex: Int, columnIndex: Int ): Object = {
    require( columnIndex == 0 )
    require( null != value )
    require( rowIndex == 0 )
    value
  }

  override def update: Seq[ValidationAnnotation] =
    if ( null != value ) {
      Seq()
    } else {
      val previousTime = System.currentTimeMillis()
      try {
        val message = computedDerivedWidget.prettyPrint( "" )+"\n"
        ClassLoaderHelper.createDynamicScriptClassLoader( computedDerivedWidget ) match {
          case Failure( t ) =>
            ClassLoaderHelper.reportError( computedDerivedWidget, message, t )
            return Seq()

          case Success( scriptCL ) => {
            val localClassLoader = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader( scriptCL )

            try {
              ClassLoaderHelper.lookupClassAndMethod( scriptCL, computedDerivedWidget,
                classOf[Project], classOf[ActionEvent], classOf[ComputedDerivedWidget],
                classOf[MagicDrawElementKindDesignation], classOf[Element] ) match {
                  case Failure( t ) =>
                    ClassLoaderHelper.reportError( computedDerivedWidget, message, t )
                    return Seq()

                  case Success( cm ) =>
                    val result = ClassLoaderHelper.invokeAndReport( previousTime, Project.getProject( e ), null, cm, ek, e )
                    result match {
                      case Failure( t ) =>
                        return Seq()
                      case Success( ( component: Component, rs: Seq[_] ) ) =>
                        DerivedPropertyComputedWidget.asSeqOfValidationAnnotation( rs ) match {
                          case None =>
                            ClassLoaderHelper.reportError( computedDerivedWidget, message, new IllegalArgumentException( s"Unrecognized result -- expected: Seq[ValidationAnnotation], got: ${rs.getClass.getName}" ) )
                            return Seq()
                          case Some( annotations ) =>
                            value = component
                            annotations
                        }
                      case Success( x ) =>
                        ClassLoaderHelper.reportError( computedDerivedWidget, message, new IllegalArgumentException( s"Unrecognized result -- expected: ( java.awt.Component, Seq[ValidationAnnotation] ), got: ${x.getClass.getName}" ) )
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

object DerivedPropertyComputedWidget {

  def asSeqOfValidationAnnotation( rs: Seq[_] ): Option[Seq[ValidationAnnotation]] =
    if ( rs.forall( _ match {
      case ( _: ValidationAnnotation ) => true
      case _                           => false
    } ) )
      Some( rs.asInstanceOf[Seq[ValidationAnnotation]] )
    else None
}