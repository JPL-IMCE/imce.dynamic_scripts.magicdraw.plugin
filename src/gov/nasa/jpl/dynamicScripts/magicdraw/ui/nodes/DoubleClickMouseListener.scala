/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes

import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

import javax.swing.JOptionPane
import javax.swing.JTable

import scala.language.postfixOps
import scala.reflect.ClassTag

import com.jidesoft.grid.JideTable
import com.nomagic.magicdraw.ui.dialogs.specifications.SpecificationDialogManager
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DoubleClickMouseListener[T](
  _table: JTable,
  callback: PartialFunction[( T, MouseEvent, Int, Int ), Unit] )( implicit tag: ClassTag[T] )
  extends MouseListener() {

  override def mouseClicked( ev: MouseEvent ): Unit =
    if ( ev.getClickCount() == 2 && ev.getButton() == MouseEvent.BUTTON1 ) {
      val source = ev.getSource()
      require( _table.equals( source ) )
      val row = _table.rowAtPoint( ev.getPoint() )
      val col = _table.columnAtPoint( ev.getPoint() )
      _table.getModel.getValueAt( row, col ) match {
        case t: T => callback.lift( t, ev, row, col )
        case _    => ()
      }
    }

  override def mousePressed( ev: MouseEvent ): Unit = ()
  override def mouseReleased( ev: MouseEvent ): Unit = ()
  override def mouseEntered( ev: MouseEvent ): Unit = ()
  override def mouseExited( ev: MouseEvent ): Unit = ()
}

object DoubleClickMouseListener {

  def createAbstractTreeNodeInfoDoubleClickMouseListener( table: JideTable ): DoubleClickMouseListener[AbstractTreeNodeInfo] =
    DoubleClickMouseListener[AbstractTreeNodeInfo](
      table,
      {
        case ( n: AnnotationNodeInfo, ev: MouseEvent, row: Int, col: Int ) =>
          n.getAnnotation.getTarget match {
            case e: Element =>
              if ( ( ev.getModifiersEx & InputEvent.SHIFT_DOWN_MASK ) == InputEvent.SHIFT_DOWN_MASK ) {
                JOptionPane.showMessageDialog( table,
                  s"${e.getHumanType}: '${e.getHumanName}' =>\n${n.getAnnotation.getText}",
                  n.getAnnotationKind,
                  n.getAnnotationMessageKind,
                  DynamicScriptsPlugin.getInstance.getJPLSmallIcon )
              }
              else {
                SpecificationDialogManager.getManager.editSpecification( e )
              }
            case _ => ()
          }

        case ( n: ReferenceNodeInfo, ev: MouseEvent, row: Int, col: Int ) =>
          SpecificationDialogManager.getManager.editSpecification( n.e )

        case ( n: LabelNodeInfo, ev: MouseEvent, row: Int, col: Int ) =>
          ( for {
            c <- 0 until table.getModel.getColumnCount
            v = table.getModel.getValueAt( row, c )
            av = v match {
              case n: AnnotationNodeInfo if ( null != n.getAnnotation.getTarget ) => Some( n )
              case _ => None
            }
            if ( av.isDefined )
          } yield av.get ) toList match {
            case Nil => ()
            case n :: _ =>
              n.getAnnotation.getTarget match {
                case e: Element =>
                  if ( ( ev.getModifiersEx & InputEvent.SHIFT_DOWN_MASK ) == InputEvent.SHIFT_DOWN_MASK ) {
                    JOptionPane.showMessageDialog( table,
                      s"${e.getHumanType}: '${e.getHumanName}' =>\n${n.getAnnotation.getText}",
                      n.getAnnotationKind,
                      n.getAnnotationMessageKind,
                      DynamicScriptsPlugin.getInstance.getJPLSmallIcon )
                  }
                  else {
                    SpecificationDialogManager.getManager.editSpecification( e )
                  }
                case _ => ()
              }
          }
      } )
}