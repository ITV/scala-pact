package com.itv.scalapact.plugin.shared

import java.io.{File, PrintWriter}

//todo make defs
object PactContractWriter {

  private val simplifyName: String => String = name => "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  val writePactToFile: String => String => String => String => Unit = dirPath =>
    provider =>
      consumer =>
        contents => {
          val dirFile = new File(dirPath)

          if (!dirFile.exists()) {
            dirFile.mkdir()
          }

          val relativePath = dirPath + "/" + simplifyName(consumer) + "_" + simplifyName(provider) + ".json"
          val file         = new File(relativePath)

          if (file.exists()) {
            file.delete()
          }

          file.createNewFile()

          new PrintWriter(relativePath) {
            write(contents)
            close()
          }

          ()
  }
}
