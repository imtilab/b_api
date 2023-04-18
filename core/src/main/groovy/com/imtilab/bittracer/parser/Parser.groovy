package com.imtilab.bittracer.parser

import com.imtilab.bittracer.utils.ApplicationResource

/**
 * For all kind of file parser
 */
interface Parser {

    /**
     * set file to parse
     * @param file
     */
    abstract void setFiles(List files)

    /**
     * set resource
     * @param file
     */
    abstract void setResources(ApplicationResource resource)

    /**
     * Read file form path
     * @param readFirstLine
     * @return
     */
    abstract def loadFile(File file, boolean readFirstLine)

}