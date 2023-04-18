package com.imtilab.bittracer.test.validationv2.schemavalidator

import com.networknt.schema.ValidationMessage

class ValidationMessageBuilder {
    static ValidationMessage build(CustomValidatorTypeCode validatorTypeCode, String path){
        ValidationMessage.Builder builder = new ValidationMessage.Builder()
        builder.code(validatorTypeCode.errorCode)
                .path(path)
                .format(validatorTypeCode.messageFormat)
        builder.build()
    }
}
