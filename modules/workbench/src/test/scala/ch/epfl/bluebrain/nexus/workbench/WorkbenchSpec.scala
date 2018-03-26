package ch.epfl.bluebrain.nexus.workbench

import java.io.File

//noinspection TypeAnnotation
class WorkbenchSpec extends WorkbenchSpecLike {
  override def baseUri      = "http://localhost/v0"
  override def baseUriToken = "{{base}}"
  override def baseDir      = new File("./modules/workbench/src/test/resources").getAbsoluteFile.toPath.normalize().toString
  override def testDir      = baseDir
}