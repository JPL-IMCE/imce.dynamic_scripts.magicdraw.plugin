package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import java.io.File
import java.io.FilenameFilter
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.Traversable
import scala.collection.JavaConversions._

/**
 * @see ttp://stackoverflow.com/a/8927040/1009693
 *
 * @param path The starting path of the traversal
 * @param options @see java.nio.file.FileVisitOption
 * @param maxDepth The maximum depth of the traversal from the starting path
 */
class TraversePath(path: Path, options: Set[FileVisitOption], maxDepth: Int) extends Traversable[(Path, BasicFileAttributes)] {

  override def foreach[U](f: ((Path, BasicFileAttributes)) => U): Unit = {

    class Visitor extends SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = try {
        f(file -> attrs)
        FileVisitResult.CONTINUE
      } catch {
        case _: Throwable => FileVisitResult.TERMINATE
      }
    }

    Files.walkFileTree(path, options, maxDepth, new Visitor)
  }

}

object TraversePath {

  def listFilesRecursively(dir: File,
                           filter: FilenameFilter,
                           options: Set[FileVisitOption] = Set(FileVisitOption.FOLLOW_LINKS),
                           maxDepth: Int = 5): List[File] = {
    val traversal = new TraversePath(dir.toPath, options, maxDepth)
    val matchingFiles = new scala.collection.mutable.ListBuffer[File]()
    traversal.foreach {
      case (p, _) =>
        val f = p.toFile
        if (filter.accept(f.getParentFile, f.getName)) matchingFiles.add(f)
    }
    matchingFiles.toList
  }

}