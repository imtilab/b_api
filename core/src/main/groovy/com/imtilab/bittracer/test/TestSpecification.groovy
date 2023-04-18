package com.imtilab.bittracer.test

import com.imtilab.bittracer.constant.Method

interface TestSpecification {

    /**
     * In endpoint, if API has version support then version must be declare as ${version}*
     * Which will later replace from the value of test case
     * @return API end
     */
    abstract String endPoint()

    /**
     *
     * @return enum Method
     */
    abstract Method method()

}