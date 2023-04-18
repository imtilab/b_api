package com.imtilab.bittracer.test

import com.imtilab.bittracer.test.ECSGSpecification
import com.imtilab.bittracer.constant.Method

class GraphQLSpecification extends ECSGSpecification{
    @Override
    String endPoint() {
        return ""
    }

    @Override
    Method method() {
        return Method.POST
    }
}
