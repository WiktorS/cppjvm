package com.earwicker.cppjvm.cppwrap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SourceGenerator {
    protected static final int DECLARE_TYPES = 0;
    protected static final int CALL_WRAPPED = 1;
    protected static final int CALL_UNWRAPPED = 2;
    private PrintWriter writer;
    private Class<?> source;

    public String toString(Class<?> cls) throws Exception {
        source = cls;
        final Writer result = new StringWriter();
        writer = new PrintWriter(result);
        editWarning();
        generate();
        return result.toString();
    }

    protected PrintWriter out() {
        return writer;
    }

    protected Class<?> cls() {
        return source;
    }

    public abstract void generate() throws Exception;

    private void editWarning() {
        out().println();
        out().println("//");
        out().println("// Do not edit this file - it was generated automatically from the Java class:");
        out().println("//");
        out().println("// " + cls().getName());
        out().println("//");
        out().println();
    }

    protected void include(Class<?> cls) {
        if (CppWrap.isWrapped(cls))
            out().println("#include <" + cls.getName().replace('.', '/').replace('$', '/') + ".hpp>");
    }

    protected void includeRequiredTypes() throws Exception {
        for (Class<?> required : CppWrap.getDirectlyRequiredTypes(cls())) {
            include(required);
        }
    }

    protected void beginNamespace(Class<?> cls) {

        out().println("// name: " + cls.getName());
        String[] namespaces = cls.getName().split("\\.");
        for (int n = 0; n < (namespaces.length - 1); n++)
            out().print(" namespace " + namespaces[n] + " {");

        List<String> names = new ArrayList<String>();
        for (Class<?> c = cls.getDeclaringClass(); c != null; c = c.getDeclaringClass()) {
            out().println("// declaringClass: " + c.getName());
            names.add(c.getSimpleName() + "_n");
        }
        Collections.reverse(names);
        for (String name : names) {
            out().print(" namespace " + name + " {");
        }
    }

    protected void endNamespace(Class<?> cls) {

        int count = cls.getName().split("\\.").length - 1;
        for (Class<?> c = cls.getDeclaringClass(); c != null; c = c.getDeclaringClass()) {
            count++;
        }

        for (int n = 0; n < count; n++)
            out().print(" }");

        out().println();
    }

    protected void listParameters(Class<?>[] params, int mode) throws Exception {
        int pos = 0;
        for (Class<?> p : params) {
            pos++;
            out().print(
                (pos > 1 ? ", " : "") +
                    (mode == DECLARE_TYPES
                        ? (CppWrap.isWrapped(p) ? ("const " + CppWrap.cppType(p) + " &")
                        : (CppWrap.cppType(p) + " "))
                        : "") +
                    "args" + pos +
                    (mode == CALL_UNWRAPPED && CppWrap.isWrapped(p) ? ".get_impl()" : "")
            );
        }
    }

    protected boolean isFieldHidden(Field field) {
        for (Field f : cls().getFields()) {
            if (!f.equals(field) &&
                f.getName().equals(field.getName()) &&
                field.getDeclaringClass().isAssignableFrom(f.getDeclaringClass()))
                return true;
        }
        return false;
    }
}


