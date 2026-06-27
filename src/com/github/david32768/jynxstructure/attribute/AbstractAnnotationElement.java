package com.github.david32768.jynxstructure.attribute;

import com.github.david32768.jynxstructure.*;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;

public abstract class AbstractAnnotationElement extends SimpleAttributeElement {

    public AbstractAnnotationElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        super(ptr, entry, buffer);
    }

    protected void checkAnnotation() {
        CPEntry cpe = buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // type
        checkAnnotationValues();
    }

    protected void checkAnnotationValues() {
        int valuect = buffer.nextUnsignedShort();
        for (int i = 0; i < valuect; ++i) {
            buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // name
            checkElementValue();
        }
    }

    protected void checkArrayValues() {
        int valuect = buffer.nextUnsignedShort();
        for (int i = 0; i < valuect; ++i) {
            checkElementValue();
        }
    }

    protected void checkElementValue() {
        char tag = (char)buffer.nextUnsignedByte();
        switch(tag) {
            case 'B', 'C', 'I', 'S', 'Z' -> buffer.nextCPEntry(ConstantPoolType.CONSTANT_Integer);
            case 'J' -> buffer.nextCPEntry(ConstantPoolType.CONSTANT_Long);
            case 'F' -> buffer.nextCPEntry(ConstantPoolType.CONSTANT_Float);
            case 'D' -> buffer.nextCPEntry(ConstantPoolType.CONSTANT_Double);
            case 's' -> buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8);
            case 'e' -> {
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // type
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // name
            }
            case 'c' -> buffer.nextCPEntry(ConstantPoolType.CONSTANT_Utf8); // name
            case '@' -> checkAnnotation();
            case '[' -> checkArrayValues();
            default -> {
                String msg = String.format("unknown annotation tag '%c'", tag);
                throw new IllegalArgumentException(msg);
            }
        }
    }

}