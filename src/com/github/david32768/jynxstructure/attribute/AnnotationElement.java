package com.github.david32768.jynxstructure.attribute;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxstructure.AttributeBuffer;
import com.github.david32768.jynxstructure.CPEntry;
import com.github.david32768.jynxstructure.IndentPrinter;

public class AnnotationElement extends AbstractAnnotationElement {

    public AnnotationElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        super(ptr, entry, buffer);
    }
    
    @Override
    public void check() {
        CPEntry cpe = buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // type
        checkAnnotationValues();
    }

}
