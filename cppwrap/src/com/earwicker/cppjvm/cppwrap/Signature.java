package com.earwicker.cppjvm.cppwrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

class Signature {
    private static final java.util.HashMap<String, String> primitives;

    static {
        primitives = new java.util.HashMap<String, String>();
        primitives.put("void", "V");
        primitives.put("boolean", "Z");
        primitives.put("int", "I");
        primitives.put("byte", "B");
        primitives.put("short", "S");
        primitives.put("long", "J");
        primitives.put("double", "D");
        primitives.put("float", "F");
        primitives.put("char", "C");
    }

    private static String generate(Class[] p, String r) {
        StringBuilder b = new StringBuilder();

        b.append("(");

        for (Class aP : p) b.append(generate(aP));

        b.append(")");
        b.append(r);
        return b.toString();
    }

    static String generate(Method m) {
        return generate(m.getParameterTypes(),
            generate(m.getReturnType()));
    }

    static String generate(Constructor m) {
        return generate(m.getParameterTypes(), "V");
    }

    static String generate(Class c) {
        if (c.isPrimitive())
            return primitives.get(c.toString());
        else if (c.isArray())
            return "[" + generate(c.getComponentType());
        else
            return "L" + (c.getName()).replace('.', '/') + ";";
    }
}
