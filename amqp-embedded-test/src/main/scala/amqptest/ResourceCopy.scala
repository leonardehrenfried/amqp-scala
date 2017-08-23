package amqptest

import java.io.{ File, FileInputStream, FileOutputStream, InputStream }
import java.net.{ JarURLConnection, URL }

import sun.net.www.protocol.file.FileURLConnection

import scala.collection.JavaConverters._

object ResourceCopy {

  def copyFilesRecursively(origin: File, destination: File): Unit = origin match {
    case dir if dir.isDirectory ⇒
      destination.exists() || destination.mkdir()
      assert(destination.isDirectory)
      dir.list().foreach(filename ⇒
        copyFilesRecursively(new File(origin, filename), new File(destination, filename)))
    case file if file.isFile ⇒
      val in = new FileInputStream(file)
      val out = new FileOutputStream(destination)
      streamAll(in, out)
      in.close()
      out.close()
  }

  // an example of crappy integration between java8 and scala
  def copyJarResourcesRecursively(connection: JarURLConnection, destination: File) = {
    val jarEntries = connection.getJarFile.entries()
    jarEntries.asScala.foreach {
      case nonmatch if !nonmatch.getName.startsWith(connection.getEntryName) ⇒
      case inFile if !inFile.isDirectory ⇒
        val destFile = new File(destination, inFile.getName.substring(connection.getEntryName.length))
        val in = connection.getJarFile.getInputStream(inFile)
        val out = new FileOutputStream(destFile)
        streamAll(in, out)
        in.close()
        out.close()
      case _ ⇒
    }
  }

  def streamAll(in: InputStream, out: FileOutputStream) {
    Iterator.continually(in.read()).takeWhile(-1 != _).foreach(out.write)
  }

  def copyResourcesRecursively(originUrl: URL, destination: File) =
    originUrl.openConnection() match {
      case jarConnection: JarURLConnection ⇒
        copyJarResourcesRecursively(jarConnection, destination);
      case _: FileURLConnection ⇒
        copyFilesRecursively(new File(originUrl.getPath), destination);
      case urlConnection ⇒
        throw new Exception("URLConnection[" + urlConnection.getClass.getSimpleName +
          "] is not a recognized/implemented connection type.");
    }
}
