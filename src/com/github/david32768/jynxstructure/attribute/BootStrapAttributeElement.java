package com.github.david32768.jynxstructure.attribute;

import static com.github.david32768.jynxfree.jynx.Global.JVM_VERSION;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxstructure.my.Message.M507;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxstructure.AttributeBuffer;
import com.github.david32768.jynxstructure.CPEntry;
import com.github.david32768.jynxstructure.IndentPrinter;

public class BootStrapAttributeElement extends SimpleAttributeElement {
    
    public BootStrapAttributeElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        super(ptr, entry, buffer);
    }
    
    @Override
    public void check() {
        CPEntry methodcp = buffer.nextCPEntry(ConstantPoolType.CONSTANT_MethodHandle);
        JvmVersion jvmversion = JVM_VERSION();
        int argct = buffer.nextUnsignedShort();
        CPEntry[] entries = new CPEntry[1 + argct];
        entries[0] = methodcp;
        for (int k = 0; k < argct; ++k) {
            CPEntry argcp = buffer.nextCPEntry();
            ConstantPoolType cptk = argcp.getType();
            if (!cptk.isLoadableBy(jvmversion)) {
                // "boot argument %s is not loadable by %s"
                LOG(M507, cptk, jvmversion);
            }
            entries[k + 1] = argcp;
        }
        buffer.pool().addBootstrap(entries);
    }
    
}
