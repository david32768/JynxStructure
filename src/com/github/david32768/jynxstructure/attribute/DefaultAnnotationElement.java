package com.github.david32768.jynxstructure.attribute;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxstructure.AttributeBuffer;
import com.github.david32768.jynxstructure.IndentPrinter;

public class DefaultAnnotationElement extends AbstractAnnotationElement {

    public DefaultAnnotationElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        super(ptr, entry, buffer);
    }
    
    @Override
    public void check() {
        checkElementValue();
    }

}
