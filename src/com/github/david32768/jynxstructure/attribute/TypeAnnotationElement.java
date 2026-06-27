package com.github.david32768.jynxstructure.attribute;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxstructure.my.Message.M523;
import static com.github.david32768.jynxstructure.my.Message.M526;
import static com.github.david32768.jynxstructure.my.Message.M527;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.TypeRef;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;
import com.github.david32768.jynxstructure.AttributeBuffer;
import com.github.david32768.jynxstructure.IndentPrinter;

public class TypeAnnotationElement extends AbstractAnnotationElement {

    public TypeAnnotationElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        super(ptr, entry, buffer);
    }
    
    @Override
    public void check() {
        int target_type = buffer.nextUnsignedByte();
        TypeRef typeref = TypeRef.fromJVM(target_type);
        Context expected = typeref.context();
        if (expected == Context.CATCH) {
            expected = Context.CODE;
        }
        Context context = buffer.context();
        if (expected != context && expected != Context.FIELD && context != Context.COMPONENT) {
            // "typeref %s (%#x) not valid in context %s"
            throw new LogIllegalArgumentException(M523, typeref, target_type, context);
        }
        switch(typeref) {
            case trc_param, trm_param ->
                // type_parameter_target
                buffer.nextUnsignedByte();
            case trc_extends ->
                // supertype_target
                buffer.nextUnsignedShort();
            case trc_param_bound, trm_param_bound -> {
                // type_parameter_bound_target
                buffer.nextUnsignedByte();
                buffer.nextUnsignedByte();
            }
            case trf_field, trm_return, trm_receiver -> {
                // empty_target
            }
            case trm_formal ->
                // formal_parameter_target
                buffer.nextUnsignedByte();
            case trm_throws ->
                // throws_target
                buffer.nextUnsignedShort();
            case tro_var, tro_resource -> {
                // local_var_target
                int table_length = buffer.nextUnsignedShort();
                for (int i = 0; i < table_length; ++i) {
                    buffer.nextUnsignedShort();
                    buffer.nextUnsignedShort();
                    buffer.nextUnsignedShort();
                }
            }
            case trt_except ->
                // catch_target
                buffer.nextUnsignedShort();
            case tro_instanceof, tro_new, tro_newref, tro_methodref ->
                // offset_target
                buffer.asCodeBuffer().nextLabel();
            case tro_cast, tro_argnew, tro_argmethod, tro_argnewref, tro_argmethodref -> {
                // type_argument target
                buffer.nextUnsignedShort();
                buffer.nextUnsignedByte();
            }
            default -> throw new LogUnexpectedEnumValueException(typeref);
        }
        // type path
        int path_length = buffer.nextUnsignedByte();
        for (int i = 0; i < path_length; ++i) {
            int type_path_kind = buffer.nextUnsignedByte();
            if (type_path_kind > 4) {
                // "type_path_kind = %d is not in range [0,3]"
                LOG(M526, type_path_kind);
            }
            int type_argument_index = buffer.nextUnsignedByte();
            if (type_argument_index != 0 && type_path_kind != 3) {
                // "type_argument_index is %d but must be 0 for type_path_kind [0,2]"
                LOG(M527, type_argument_index);
            }
        }
        checkAnnotation();
    }

}
