package com.imtilab.bittracer.gg.brandlist

import com.imtilab.bittracer.constant.Method
import com.imtilab.bittracer.test.ECSGSpecification

class TestBrandList extends ECSGSpecification {

    @Override
    String endPoint() {
        return '/${apiCallType}'
    }

    @Override
    Method method() {
        return Method.POST
    }
}