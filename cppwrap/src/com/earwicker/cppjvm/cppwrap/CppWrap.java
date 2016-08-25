package com.earwicker.cppjvm.cppwrap;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class CppWrap {
    private static final java.util.HashMap<Class<?>, String> primitives;
    private static final java.util.HashSet<String> reserved;

    private static PrintWriter log;

    static {
        reserved = new java.util.HashSet<String>();
        reserved.add("delete");
        reserved.add("union");
        reserved.add("and");
        reserved.add("or");
        reserved.add("xor");
        reserved.add("not");
        reserved.add("NULL");
        reserved.add("register");
        reserved.add("min");
        reserved.add("max");

        primitives = new java.util.HashMap<Class<?>, String>();
        primitives.put(void.class, "void");
        primitives.put(boolean.class, "jboolean");
        primitives.put(int.class, "jint");
        primitives.put(byte.class, "jbyte");
        primitives.put(short.class, "jshort");
        primitives.put(long.class, "jlong");
        primitives.put(double.class, "jdouble");
        primitives.put(float.class, "jfloat");
        primitives.put(char.class, "jchar");
    }

    private static void println(String str) {
        if (log != null)
            log.println(str);
    }

    private static String nestedName(Class<?> cls, boolean namespace) {

        String name = cls.getSimpleName();

        while (cls.getDeclaringClass() != null) {
            cls = cls.getDeclaringClass();
            name = cls.getSimpleName() + "_n::" + name;
        }

        if (namespace) {
            String packageName = cls.getPackage().getName();
            name = packageName.replaceAll("\\.", "::") + "::" + name;
        }

        return name;
    }

    static String cppType(Class<?> j) throws Exception {
        if (j == null)
            return "jobject";

        if (j.isPrimitive()) {
            String g = primitives.get(j);
            return g == null ? "jobject" : g;
        }

        if (j.isArray())
            return "::jvm::array< " + cppType(j.getComponentType()) + " >";

        return "::" + nestedName(j, true);
    }

    static boolean isWrapped(Class<?> cls) {
        return !cls.isPrimitive();
    }

    static String fixName(String name) {
        return reserved.contains(name) ? name + "_" : name;
    }

    static String fixVal(String val, Class<?> type) {
        if (type.equals(Integer.TYPE) && val.equals(String.valueOf(Integer.MIN_VALUE)))
            return "-2147483647-1"; // This fixes problem with -2147483648 on MSVC
        if (type.equals(Character.TYPE)) return "" + (int) val.charAt(0);
        return val;
    }

    private static void save(String path, String content) throws Exception {
        new File(path).getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(path);
        writer.print(content);
        writer.close();
    }

    private static String load(String path) throws Exception {
        if (!new File(path).exists())
            return "$$$empty$$$";
        StringBuilder fileData = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1)
            fileData.append(buf, 0, numRead);
        reader.close();
        return fileData.toString();
    }

    private static int saveIfDifferent(String path, String content) throws Exception {
        String oldContent = load(path);
        if (!oldContent.equals(content)) {
            println("Saving new version: " + path);
            save(path, content);
            return 1;
        }
        return 0;
    }

    private static int generate(Class<?> cls, File out,
                                List<String> files, boolean generating) throws Exception {
        int generated = 0;
        if (!isWrapped(cls))
            return generated;

        char sl = File.separatorChar;

        String headerName = out.getPath() + sl + "include" + sl +
            cls.getName().replace('.', sl).replace('$', sl) + ".hpp";

        if (generating) {
            generated += saveIfDifferent(headerName,
                new HeaderGenerator().toString(cls));
        }

        String implName = out.getPath() + sl +
            cls.getName().replace('.', '_').replace('$', '_') + ".cpp";

        if (generating) {
            generated += saveIfDifferent(implName,
                new ImplementationGenerator().toString(cls));
        }

        files.add(implName);

        return generated;
    }

    // Returns a list of types that the given type is assignment-compatible with
    static Collection<Class<?>> getSuperTypes(Class<?> cls) {
        List<Class<?>> compatibleTypes = new ArrayList<Class<?>>();

        Collections.addAll(compatibleTypes, cls.getInterfaces());

        for (Class<?> superCls = cls.getSuperclass();
             superCls != null; superCls = superCls.getSuperclass())
            compatibleTypes.add(superCls);

        return sortClasses(compatibleTypes);
    }

    // Recursively builds the set of types that are referred to in the definition of
    // the given type.
    private static void getRequiredTypes(Class<?> cls, Set<Class<?>> required, int currentDepth, Integer maxDepth) {
        if (cls.isArray()) {
            getRequiredTypes(cls.getComponentType(), required, currentDepth, maxDepth); // same depth
            return;
        }

        println("Considering: " + cls.getName());

        if (cls.isPrimitive()) {
            println("Ignoring primitive: " + cls.getName());
            return;
        }

        if (required.contains(cls)) {
            println("Ignoring already found: " + cls.getName());
            return;
        }

        println("Requires: " + cls.getName());

        required.add(cls);

        if (maxDepth == null || currentDepth < maxDepth) {

            for (Class<?> st : CppWrap.getSuperTypes(cls)) {
                println("Super classes of: " + cls.getName());
                getRequiredTypes(st, required, currentDepth + 1, maxDepth);
            }

            println("Constructors of: " + cls.getName());
            for (Constructor<?> ctor : cls.getConstructors()) {
                for (Class<?> p : ctor.getParameterTypes())
                    getRequiredTypes(p, required, currentDepth + 1, maxDepth);
            }

            println("Methods of: " + cls.getName());
            for (Method m : cls.getMethods()) {
                println("Method " + cls.getName() + "." + m.getName());
                getRequiredTypes(m.getReturnType(), required, currentDepth + 1, maxDepth);
                for (Class<?> p : m.getParameterTypes())
                    getRequiredTypes(p, required, currentDepth + 1, maxDepth);
            }

            println("Fields of: " + cls.getName());
            for (Field f: cls.getFields()) {
                println("Field " + cls.getName() + "." + f.getName());
                getRequiredTypes(f.getType(), required, currentDepth + 1, maxDepth);
            }

            for (Class<?> c : cls.getClasses()) {
                println("Nested classes of: " + cls.getName());
                getRequiredTypes(c, required, currentDepth, maxDepth); // same depth
            }
        }
    }

    private static void getAllRequiredTypes(Class<?> cls, Set<Class<?>> required) {
        getRequiredTypes(cls, required, 0, null);
    }

    static Collection<Class<?>> getDirectlyRequiredTypes(Class<?> cls) {

        println("Directly required types for: " + cls.getName());

        HashSet<Class<?>> req = new HashSet<Class<?>>();
        getRequiredTypes(cls, req, 0, 1);

        return sortClasses(req);
    }

    private static List<Class<?>> sortClasses(Collection<Class<?>> classes) {
        List<Class<?>> sorted = new ArrayList<Class<?>>(classes);
        Collections.sort(sorted, new Comparator<Class<?>>() {
            public int compare(Class<?> arg0, Class<?> arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        });
        return sorted;
    }

    static List<Constructor<?>> sortConstructors(Constructor<?>[] constructors) {
        List<Constructor<?>> sorted = Arrays.asList(constructors);
        Collections.sort(sorted, new Comparator<Constructor<?>>() {
            public int compare(Constructor<?> arg0, Constructor<?> arg1) {
                return arg0.toGenericString().compareTo(arg1.toGenericString());
            }
        });
        return sorted;
    }

    static Collection<Method> sortMethods(Method[] methods) {
        List<Method> sorted = Arrays.asList(methods);
        Collections.sort(sorted, new Comparator<Method>() {
            public int compare(Method arg0, Method arg1) {
                return arg0.toGenericString().compareTo(arg1.toGenericString());
            }
        });
        return sorted;
    }


    static Collection<Field> sortFields(Field[] fields) {
        List<Field> sorted = Arrays.asList(fields);
        Collections.sort(sorted, new Comparator<Field>() {
            public int compare(Field arg0, Field arg1) {
                return arg0.toGenericString().compareTo(arg1.toGenericString());
            }
        });
        return sorted;
    }

    static boolean isMethodDuplicated(Collection<Method> methods, Method referenceMethod) {
        String referenceSignature = Signature.generate(referenceMethod);
        for (Method method : methods) {
            if (method != referenceMethod
                && method.getName().equals(referenceMethod.getName())
                && referenceSignature.equals(Signature.generate(method)))
                return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2)
            System.err.print("Please specify output-path and one or more Java class names");
        else {
            File outDir = new File(args[0]);

            boolean generating = true;

            Set<Class<?>> required = new HashSet<Class<?>>();
            for (int a = 1; a < args.length; a++) {
                if (args[a].equals("--log")) {
                    if (log == null) {
                        outDir.mkdirs();
                        log = new PrintWriter(new FileWriter(new File(outDir, "CppWrapLog.txt")));
                    }
                } else if (args[a].equals("--list")) {
                    generating = false;
                } else if (args[a].equals("--generate")) {
                    generating = true;
                } else {
                    getAllRequiredTypes(Class.forName(args[a]), required);
                }
            }

            int count = 0;
            List<String> files = new ArrayList<String>();
            for (Class<?> cls : sortClasses(required))
                count += generate(cls, outDir, files, generating);

            if (generating) {
                if (count == 0)
                    println("All wrapped Java classes were already up to date");
            } else {
                for (String file : files) {
                    System.out.print(file.replace(File.separatorChar, '/') + " ");
                }
            }

            if (log != null)
                log.close();
        }
    }
}

