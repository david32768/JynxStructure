package com.github.david32768.jynxstructure.attribute;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxstructure.AttributeBuffer;
import com.github.david32768.jynxstructure.IndentPrinter;

public class ParameterAnnotationElement extends AbstractAnnotationElement {

    public ParameterAnnotationElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        super(ptr, entry, buffer);
    }
    
    @Override
    public void check() {
        int valuect = buffer.nextUnsignedShort();
        for (int i = 0; i < valuect; ++i) {
            checkAnnotation();
        }
    }

}
