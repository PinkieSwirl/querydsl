/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.codegen;

import static com.mysema.codegen.Symbols.ASSIGN;
import static com.mysema.codegen.Symbols.COMMA;
import static com.mysema.codegen.Symbols.DOT;
import static com.mysema.codegen.Symbols.QUOTE;
import static com.mysema.codegen.Symbols.SEMICOLON;
import static com.mysema.codegen.Symbols.SPACE;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.mysema.codegen.model.Type;


/**
 * JavaWriter is the default implementation of the CodeWriter interface
 * 
 * @author tiwe
 *
 */
public final class JavaWriter extends AbstractCodeWriter<JavaWriter>{
    
    private static final String EXTENDS = " extends ";
        
    private static final String IMPLEMENTS = " implements ";

    private static final String IMPORT = "import ";

    private static final String IMPORT_STATIC = "import static ";

    private static final String PACKAGE = "package ";

    private static final String PRIVATE = "private ";
    
    private static final String PRIVATE_FINAL = "private final ";
    
    private static final String PRIVATE_STATIC_FINAL = "private static final ";

    private static final String PROTECTED = "protected ";
    
    private static final String PROTECTED_FINAL = "protected final ";

    private static final String PUBLIC = "public ";

    private static final String PUBLIC_CLASS = "public class ";

    private static final String PUBLIC_FINAL = "public final ";

    private static final String PUBLIC_INTERFACE = "public interface ";

    private static final String PUBLIC_STATIC = "public static ";

    private static final String PUBLIC_STATIC_FINAL = "public static final ";
    
    private final Set<String> importedClasses = new HashSet<String>();
    
    private final Set<String> importedPackages = new HashSet<String>();
    
    private Type type;
    
    public JavaWriter(Appendable appendable){
        super(appendable);
        this.importedPackages.add("java.lang");
    }
    
    @Override
    public JavaWriter annotation(Annotation annotation) throws IOException {
        beginLine().append("@").appendType(annotation.annotationType());
        Method[] methods = annotation.annotationType().getDeclaredMethods();
        if (methods.length == 1 && methods[0].getName().equals("value")){
            try {
                Object value = methods[0].invoke(annotation);
                append("(");
                annotationConstant(value);
                append(")");
            } catch (IllegalArgumentException e) {
                throw new CodegenException(e);
            } catch (IllegalAccessException e) {
                throw new CodegenException(e);
            } catch (InvocationTargetException e) {
                throw new CodegenException(e);
            }            
        }else{
            boolean first = true;        
            for (Method method : methods){            
                try {
                    Object value = method.invoke(annotation);
                    if (value == null || value.equals(method.getDefaultValue())){
                        continue;
                    }else if (!first){
                        append(COMMA);
                    }else{
                        append("(");
                    }
                    append(method.getName()+"=");                
                    annotationConstant(value);
                } catch (IllegalArgumentException e) {
                    throw new CodegenException(e);
                } catch (IllegalAccessException e) {
                    throw new CodegenException(e);
                } catch (InvocationTargetException e) {
                    throw new CodegenException(e);
                }
                first = false;
            }               
            if (!first){
                append(")");    
            }        
        }        
        return nl();
     }    
    

    @Override
    public JavaWriter annotation(Class<? extends Annotation> annotation) throws IOException{
        return beginLine().append("@").appendType(annotation).nl();
    }
    
    @SuppressWarnings("unchecked")
    private void annotationConstant(Object value) throws IOException{
         if (value instanceof Class){
             appendType((Class)value).append(".class");             
         }else if (value instanceof Number || value instanceof Boolean){
             append(value.toString());
         }else if (value instanceof Enum){
             Enum enumValue = (Enum)value;
             append(enumValue.getDeclaringClass().getName()+DOT+enumValue.name());
         }else if (value instanceof String){
             append(QUOTE + StringEscapeUtils.escapeJava(value.toString()) + QUOTE);
         }else{
             throw new IllegalArgumentException("Unsupported annotation value : " + value);
         }
     }
    
    private JavaWriter appendType(Class<?> type) throws IOException{
        if (importedClasses.contains(type.getName()) || importedPackages.contains(type.getPackage().getName())){
            append(type.getSimpleName());
        }else{
            append(type.getName());
        }
        return this;
    }

    @Override
    public JavaWriter beginClass(Type type) throws IOException{
        return beginClass(type, null);
    }
    
    @Override
    public JavaWriter beginClass(Type type, Type superClass, Type... interfaces) throws IOException{
        importedPackages.add(type.getPackageName());
        beginLine(PUBLIC_CLASS + type.getSimpleName());
        if (superClass != null){
            append(EXTENDS + superClass.getGenericName(false, importedPackages, importedClasses));
        }
        if (interfaces.length > 0){
            append(IMPLEMENTS);
            for (int i = 0; i < interfaces.length; i++){
                if (i > 0){
                    append(COMMA);
                }
                append(interfaces[i].getGenericName(false, importedPackages, importedClasses));
            }
        }
        append(" {").nl().nl();
        goIn();
        this.type = type;
        return this;
    }
 
    @Override
    public <T> JavaWriter beginConstructor(Collection<T> parameters, Transformer<T,String> transformer) throws IOException {
        beginLine(PUBLIC + type.getSimpleName()).params(parameters, transformer).append(" {").nl();
        return goIn();        
    }
    
    @Override
    public JavaWriter beginConstructor(String... parameters) throws IOException{
        beginLine(PUBLIC + type.getSimpleName()).params(parameters).append(" {").nl();
        return goIn();
    }
    
    @Override
    public JavaWriter beginInterface(Type type, Type... interfaces) throws IOException {
        importedPackages.add(type.getPackageName());
        beginLine(PUBLIC_INTERFACE + type.getGenericName(false, importedPackages, importedClasses));
        if (interfaces.length > 0){
            append(EXTENDS);
            for (int i = 0; i < interfaces.length; i++){
                if (i > 0){
                    append(COMMA);
                }
                append(interfaces[i].getGenericName(false, importedPackages, importedClasses));
            }
        }
        append(" {").nl().nl();
        goIn();
        this.type = type;
        return this;        
    }
    
    private JavaWriter beginMethod(String modifiers, Type returnType, String methodName, String... args) throws IOException{
        beginLine(modifiers + returnType + SPACE + methodName).params(args).append(" {").nl();
        return goIn();
    }
    
    @Override
    public <T> JavaWriter beginPublicMethod(Type returnType, String methodName, Collection<T> parameters, Transformer<T, String> transformer) throws IOException {
        return beginMethod(PUBLIC, returnType, methodName, transform(parameters, transformer));
    }

    @Override
    public JavaWriter beginPublicMethod(Type returnType, String methodName, String... args) throws IOException{
        return beginMethod(PUBLIC, returnType, methodName, args);
    }
    
    @Override
    public <T> JavaWriter beginStaticMethod(Type returnType, String methodName, Collection<T> parameters, Transformer<T, String> transformer) throws IOException {
        return beginMethod(PUBLIC_STATIC, returnType, methodName, transform(parameters, transformer));
    }

    @Override
    public JavaWriter beginStaticMethod(Type returnType, String methodName, String... args) throws IOException{
        return beginMethod(PUBLIC_STATIC, returnType, methodName, args);
    }
    
    @Override
    public JavaWriter end() throws IOException{
        goOut();
        return line("}").nl();
    }
 
    @Override
    public JavaWriter field(Type type, String name) throws IOException {
        return line(type.getGenericName(false, importedPackages, importedClasses) + SPACE + name + SEMICOLON).nl();
    }

    private JavaWriter field(String modifier, Type type, String name) throws IOException{
        return line(modifier + type.getGenericName(false, importedPackages, importedClasses) + SPACE + name + SEMICOLON).nl();
    }
    
    private JavaWriter field(String modifier, Type type, String name, String value) throws IOException{
        return line(modifier + type.getGenericName(false, importedPackages, importedClasses) + SPACE + name + ASSIGN + value + SEMICOLON).nl();
    }

    @Override
    public JavaWriter imports(Class<?>... imports) throws IOException{
        for (Class<?> cl : imports){
            importedClasses.add(cl.getName());
            line(IMPORT + cl.getName() + SEMICOLON);
        }
        nl();
        return this;
    }

    @Override
    public JavaWriter imports(Package... imports) throws IOException {
        for (Package p : imports){
            importedPackages.add(p.getName());
            line(IMPORT + p.getName() + ".*;");
        }
        nl();
        return this;
    }
    
    @Override
    public JavaWriter importClasses(String... imports) throws IOException{
        for (String cl : imports){
            importedClasses.add(cl);
            line(IMPORT + cl + SEMICOLON);
        }
        nl();
        return this;
    }

    @Override
    public JavaWriter importPackages(String... imports) throws IOException {
        for (String p : imports){
            importedPackages.add(p);
            line(IMPORT + p + ".*;");
        }
        nl();
        return this;
    }

    @Override
    public JavaWriter javadoc(String... lines) throws IOException {
        line("/**");
        for (String line : lines){
            line(" * " + line);
        }
        return line(" */");
    }

    @Override
    public JavaWriter packageDecl(String packageName) throws IOException{
        importedPackages.add(packageName);
        return line(PACKAGE + packageName + SEMICOLON).nl();
    }
    
    private <T> JavaWriter params(Collection<T> parameters, Transformer<T,String> transformer) throws IOException{
        append("(");
        boolean first = true;
        for (T param : parameters){
            if (!first){
                append(COMMA);
            }
            append(transformer.transform(param));
            first = false;
        }
        append(")");
        return this;
    }

    private JavaWriter params(String... params) throws IOException{
        append("(");
        append(StringUtils.join(params, COMMA));
        append(")");
        return this;
    }
    
    @Override
    public JavaWriter privateField(Type type, String name) throws IOException {
        return field(PRIVATE, type, name);
    }
    
    @Override
    public JavaWriter privateFinal(Type type, String name) throws IOException {
        return field(PRIVATE_FINAL, type, name);        
    }
    
    @Override
    public JavaWriter privateFinal(Type type, String name, String value) throws IOException {
        return field(PRIVATE_FINAL, type, name, value);
    }

    @Override
    public JavaWriter privateStaticFinal(Type type, String name, String value) throws IOException {
        return field(PRIVATE_STATIC_FINAL, type, name, value);
    }
        
    @Override
    public JavaWriter protectedField(Type type, String name) throws IOException {
        return field(PROTECTED, type, name);        
    }
    
    @Override
    public JavaWriter protectedFinal(Type type, String name) throws IOException {
        return field(PROTECTED_FINAL, type, name);        
    }

    @Override
    public JavaWriter protectedFinal(Type type, String name, String value) throws IOException {
        return field(PROTECTED_FINAL, type, name, value);
    }

    @Override
    public JavaWriter publicField(Type type, String name) throws IOException {
        return field(PUBLIC, type, name);
    }
    
    @Override
    public JavaWriter publicFinal(Type type, String name) throws IOException {
        return field(PUBLIC_FINAL, type, name);        
    }
    
    @Override
    public JavaWriter publicFinal(Type type, String name, String value) throws IOException {
        return field(PUBLIC_FINAL, type, name, value);
    }
    
    @Override
    public JavaWriter publicStaticFinal(Type type, String name, String value) throws IOException {
        return field(PUBLIC_STATIC_FINAL, type, name, value);
    }

    @Override
    public JavaWriter staticimports(Class<?>... imports) throws IOException{
        for (Class<?> cl : imports){
            line(IMPORT_STATIC + cl.getName() + ".*;");
        }
        return this;
    }
    
    @Override
    public JavaWriter suppressWarnings(String type) throws IOException{
        return line("@SuppressWarnings(\"" + type +"\")");
    }

    private <T> String[] transform(Collection<T> parameters, Transformer<T,String> transformer){
        String[] rv = new String[parameters.size()];
        int i = 0; 
        for (T value : parameters){
            rv[i++] = transformer.transform(value);
        }
        return rv;
    }

}
