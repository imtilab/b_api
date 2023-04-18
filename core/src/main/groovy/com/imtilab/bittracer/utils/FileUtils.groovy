package com.imtilab.bittracer.utils

import com.imtilab.bittracer.constant.Constants

class FileUtils {
   public static def getIgnoreInFile(def path,def className){
        def folder = new File(path)
        for(File f:folder.listFiles()){
            if(f.isFile() && f.getName().contains(Constants.IGNORE_IN+className)){
               return f
            }
        }
      return new File(path+"/"+Constants.IGNORE_IN+className)
    }
}
