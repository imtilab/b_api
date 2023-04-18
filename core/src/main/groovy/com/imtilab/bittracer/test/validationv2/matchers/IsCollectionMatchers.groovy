package com.imtilab.bittracer.test.validationv2.matchers

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class IsCollectionMatchers extends TypeSafeMatcher<Collection>{

    Collection collection

    IsCollectionMatchers(Collection collection){
        this.collection = collection
    }

    @Override
    void describeTo(Description description) {
        description.appendText("Not a valid list")
    }

    @Override
    protected boolean matchesSafely(Collection collection) {
        return collection!=null && collection.size()>=0
    }

    static Matcher<Collection> isCollection(Collection collection) {
        new IsCollectionMatchers(collection)
    }
}
