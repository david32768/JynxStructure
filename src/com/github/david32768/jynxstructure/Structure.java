package com.github.david32768.jynxstructure;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.github.david32768.jynxfree.jvm.Context.*;
import static com.github.david32768.jynxfree.jynx.Global.*;
import static com.github.david32768.jynxstructure.my.Message.M500;
import static com.github.david32768.jynxstructure.my.Message.M502;
import static com.github.david32768.jynxstructure.my.Message.M503;
import static com.github.david32768.jynxstructure.my.Message.M517;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.Attribute;
import com.github.david32768.jynxfree.jvm.AttributeType;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.ClassUtil;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

public class Structure {

    private final String classname;
    private final JvmVersion jvmVersion;

    private Structure(String klass, JvmVersion jvmversion) {
        this.classname = klass;
        this.jvmVersion = jvmversion;
    }

    private static final int MAGIC = 0xcafebabe;
    
    public static void checkInstance(PrintWriter pw, String klass) throws IOException {
        IndentPrinter ptr = new IndentPrinter(pw);
        ByteBuffer bb = ByteBuffer.wrap(ClassUtil.getClassBytes(klass));
        bb = bb.asReadOnlyBuffer();
        bb.order(ByteOrder.BIG_ENDIAN);
        int qmagic = bb.getInt();
        if (qmagic != MAGIC) {
            // "magic number is %#x; should be %#x"
            throw new LogIllegalArgumentException(M500,qmagic,MAGIC);
        }
        JvmVersion jvmversion = JvmVersion.fromASM(bb.getInt());
        setJvmVersion(jvmversion);
        ptr.println("VERSION %s",jvmversion);
        int poolstart = bb.position();
        ConstantPool pool = ConstantPool.getInstance(bb,jvmversion);
        pool.check();
        int poolend = bb.position();
        ptr.println("CONSTANT POOL  entries = [1,%d] ; start = %#x length = %#x",
                pool.last(), poolstart, poolend - poolstart);
        if (OPTION(GlobalOption.DETAIL)) {
            pool.printCP(ptr,false);
        }
        Buffer buffer = new Buffer(pool,bb);
        int access = buffer.nextUnsignedShort();
        String klassname = buffer.nextClassName();
        Structure struct =  new Structure(klassname, jvmversion);
        struct.checkClass(ptr,buffer,access);
        boolean bootok = pool.checkBootstraps();
        if (!bootok) {
            pool.printCP(ptr,true);
        }
    }
    
    private void checkClass(IndentPrinter ptr, Buffer buffer, int access) {
        try {
            Context context = classname.equals("module-info")? MODULE: CLASS;
            setLoggerContext(context, buffer);
            ptr.println("CLASS %s %s ; start = %#x length = %#x",
                    classname, accessString(CLASS, access),
                    buffer.position(), buffer.remaining());
            buffer.nextOptCPEntry(ConstantPoolType.CONSTANT_Class); // super
            int ct = buffer.nextUnsignedShort();
            for (int i = 0; i < ct; ++i) {
                buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
            }
            check_fields(ptr,buffer);
            setLoggerContext(context, buffer);
            check_methods(ptr,buffer);
            setLoggerContext(context, buffer);
            check_attrs(context,ptr,buffer);
            setLoggerContext(context, buffer);
            if (buffer.hasRemaining()) {
                // "%s %s has %d extra bytes at end"
                LOG(M502, CLASS, classname, buffer.remaining());
                buffer.advanceToLimit();
            }
        } catch (ArithmeticException ex) {
            LOG(ex);
        }
    }
    
    private void check_fields(IndentPrinter ptr,Buffer buffer) {
        int ct = buffer.nextUnsignedShort();
        Context context = FIELD;
        for (int i = 0; i < ct; ++i) {
            setLoggerContext(context, buffer);
            int start_offset = buffer.position();
            int access = buffer.nextUnsignedShort();
            String name = buffer.nextUTF8();
            String type = buffer.nextUTF8();
            ptr.println("%s %s %s %s ; start = %#x", context, name, type,
                    accessString(FIELD, access), start_offset);
            check_attrs(context,ptr.shift(),buffer);
        }
    }

    private void check_methods(IndentPrinter ptr,Buffer buffer) {
        int ct = buffer.nextUnsignedShort();
        Context context = METHOD;
        for (int i = 0; i < ct; ++i) {
            setLoggerContext(context, buffer);
            int start_offset = buffer.position();
            int access = buffer.nextUnsignedShort();
            String name = buffer.nextUTF8();
            String type = buffer.nextUTF8();
            ptr.println("%s %s%s %s ; start = %#x", context, name, type,
                    accessString(METHOD, access), start_offset);
            check_attrs(context,ptr.shift(),buffer);
        }
    }

    private void check_attrs(Context context, IndentPrinter ptr, Buffer buffer) {
        Set<Attribute> attrset = new HashSet<>();
        int attrs_ct = buffer.nextUnsignedShort();
        for (int i = 0; i < attrs_ct; ++i) {
            setLoggerAttributeContext(context, buffer);
            int start_offset = buffer.position();
            String attrnamestr = buffer.nextUTF8();
            int size = buffer.nextSize();
            AttributeBuffer attrbuff = buffer.attributeBuffer(context, attrnamestr, size);
            AttributeInstance attr = AttributeInstance.getInstance(attrbuff);
            String attrdesc = attr.attrDesc(jvmVersion);
            ptr.println("%s ; start = %#x length = %#x",
                    attrdesc, start_offset, attr.sizs());
            if (!attr.isKnown()) {
                continue;
            }
            Attribute attribute = attr.attribute();
            boolean added = attrset.add(attribute);
            if (!added && attribute.isUnique()) {
                // "duplicate attribute %s in contexr %s"
                LOG(M517,attr,context);
            }
            checkAttributeStructure(ptr.shift(),attr);
            attr.checkAtLimit();
        }
    }

    private void checkAttributeStructure(IndentPrinter ptr,AttributeInstance attrx) {
        Attribute attr = attrx.attribute();
        AttributeBuffer attrbuff = attrx.buffer();
        AttributeType attrtype = attr.type();
        switch (attrtype) {
            case FIXED:
            case ARRAY1:
            case ARRAY:
            case MODULE:
                attrx.checkCPEntries(ptr);
                break;
            case CODE:
                checkCode(ptr, attrbuff);
                break;
            case RECORD:
                checkRecord(ptr,attrbuff);
                break;
            default:
                throw new EnumConstantNotPresentException(attrtype.getClass(), attrtype.name());
        }
    }

    private void checkCode(IndentPrinter ptr, AttributeBuffer attrbuff) {
        CodeBuffer codebuff = attrbuff.codeBuffer(ptr);
        int ct = codebuff.nextUnsignedShort();
        for (int i = 0; i < ct; ++i) {
            int startpc = codebuff.nextLabel();
            int endpc = codebuff.nextLabel();
            if (endpc < startpc) {
                // "startpc (%d) > endpc (%d)"
                LOG(M503, startpc, endpc);
            }
            int handlerpc = codebuff.nextLabel();
            Optional<CPEntry> optentry = codebuff.nextOptCPEntry(ConstantPoolType.CONSTANT_Class);
        }
        check_attrs(CODE, ptr, codebuff);
    }

    private void checkRecord(IndentPrinter ptr, AttributeBuffer attrbuff) {
        int ct = attrbuff.nextUnsignedShort();
        for (int j = 0; j < ct;++j) {
            int start = attrbuff.position();
            String attrname = attrbuff.nextUTF8();
            String attrdesc = attrbuff.nextUTF8();
            ptr.println("%s %s %s ; start = %#x",COMPONENT, attrname, attrdesc, start);
            check_attrs(COMPONENT,ptr.shift(),attrbuff);
        }
    }

    private String accessString(Context context, int access) {
        return AccessFlag.getEnumSet(access, context, jvmVersion).toString();
    }

    private void setLoggerContext(Context context, Buffer buffer) {
        log("", context, buffer);
    }
    
    private void setLoggerAttributeContext(Context context, Buffer buffer) {
        log(" Attribute", context, buffer);
    }
    
    private void log(String attribute, Context context, Buffer buffer) {
        String line = String.format("Context %s%s: start = %#x", context, attribute, buffer.position());
        LOGGER().setLine(line);
    }
    
    public static boolean printClassStructure(String klass, PrintWriter pw) {
        try {
            pw.println("START " + klass);
            Structure.checkInstance(pw, klass);
            pw.println("END " + klass);
        } catch(IOException ioex) {
            LOG(ioex);
            return false;
        }
        pw.flush();
        return true;
    }

}
