//
// Sources/ag/kcmedia/JavaBrowser.java: Class file for WO Component 'JavaBrowser'
// Project DevStudio
//
// Created by ak on Wed Jul 24 2002
//
package ag.kcmedia;

import com.webobjects.foundation.*;
import er.extensions.*;

import java.util.*;
import java.io.*;
import java.io.File;
import jode.bytecode.*;
import jode.decompiler.*;
import jode.type.Type;
import java.lang.reflect.Modifier;
import JavaCCtoHTML;

public class Jode extends Object {
    static final ERXLogger log = ERXLogger.getERXLogger(JavaBrowser.class,"components");

    public static NSMutableArray documentationPaths = new NSMutableArray();
    public static NSMutableDictionary allClasses = new NSMutableDictionary();
    public static NSMutableDictionary allPackages = new NSMutableDictionary();
    public static String cp;
    public static boolean initialized = false;
    
    public static void initialize() {
        if(initialized)
            return;
        initialized = true;
        ERXLocalizer l = ERXLocalizer.localizerForLanguage("English");
        jode.GlobalOptions.err = new PrintWriter(new StringWriter()); 
        documentationPaths = (NSMutableArray)l.localizedValueForKey("DocumentationPaths");

        NSArray arr = (NSArray)l.localizedValueForKey("ClassPaths");
        cp = arr.componentsJoinedByString(":");
        ClassInfo.setClassPath(cp);
        arr = (NSArray)l.localizedValueForKey("PackagesToIndex");
        for(Enumeration e = arr.objectEnumerator(); e.hasMoreElements();) {
            PackageProxy.indexPackage((String)e.nextElement(), null, true);
        }
    }

    public static PackageProxy packageProxyForName(String packageName) {
        Jode.initialize();
        return (PackageProxy)allPackages.objectForKey(packageName);
    }
    public static NSDictionary allPackages() {
        Jode.initialize();
        return allPackages;
    }
    public static NSDictionary allClasses() {
        Jode.initialize();
        return allClasses;
    }
    public static ClassProxy classProxyForName(String packageName, String className) {
        Jode.initialize();
        return classProxyForName(packageName + (packageName == "" ? "": ".") + className);
    }
    public static ClassProxy classProxyForName(String fullClassName) {
        Jode.initialize();
        return (ClassProxy)allClasses.objectForKey(fullClassName);
    }
    
    public static class PackageProxy {

        protected PackageProxy packageProxy;
        protected String name;
        protected ClassInfo classInfo;
        protected NSMutableArray subPackages = new NSMutableArray();
        protected NSMutableArray classes = new NSMutableArray();
        
        public PackageProxy(String name, PackageProxy packageProxy, ClassInfo classInfo) {
            this.packageProxy = packageProxy;
            this.name = name;
            this.classInfo = classInfo;
            if(this.packageProxy != null) {
                this.packageProxy.addSubPackage(this);
            }
        }

        public String name() {return name;}
        public PackageProxy packageProxy() {return packageProxy;}
        public ClassInfo classInfo() {return classInfo;}

        public void addClass(ClassProxy classProxy) {
            classes.addObject(classProxy);
        }
        public NSArray classes() { return classes; }

        public void addSubPackage(PackageProxy subPackageProxy) {
            subPackages.addObject(subPackageProxy);
        }
        public NSArray subPackages() { return subPackages; }

        public String toString() {return name;}
        
        public static void indexClass(String className, String packageName, int modifyTypes) {
            if(allClasses.valueForKey(className) !=  null)
                return;

            String classNameWithPackage = packageName + (packageName == "" ? "": ".") + className;

            ClassProxy classProxy = new ClassProxy(className, (PackageProxy)allPackages.valueForKey(packageName), ClassInfo.forName(classNameWithPackage));

            allClasses.takeValueForKey(classProxy,classNameWithPackage);
        }

        public static void indexPackage(String packageName, PackageProxy parentPackage, boolean indexClasses) {

            if(allPackages.valueForKey(packageName) != null)
                return;

            PackageProxy packageProxy = new PackageProxy(packageName, parentPackage, ClassInfo.forName(packageName));

            allPackages.takeValueForKey(packageProxy, packageName);

            Enumeration e = ClassInfo.getClassesAndPackages(packageName);
            while(e.hasMoreElements()) {
                String className = (String)e.nextElement();
                String classNameWithPackage = packageName + (packageName == "" ? "": ".") + className;
                if(ClassInfo.isPackage(classNameWithPackage)) {
                    indexPackage(classNameWithPackage, packageProxy, indexClasses);
                } else {
                    if(className.indexOf("$") < 0) {
                        if(indexClasses) {
                            indexClass(className, packageName, 0);
                        }
                    }
                }
            }
        }
    }
    
    public static class ClassProxy {
        protected PackageProxy packageProxy;
        protected String name;
        protected ClassInfo classInfo;
        protected String sourceCode;
        protected String documentation;
        
        public ClassProxy(String name, PackageProxy packageProxy, ClassInfo classInfo) {
            this.packageProxy = packageProxy;
            this.name = name;
            this.classInfo = classInfo;
            this.sourceCode = null;
            if(this.packageProxy != null) {
                this.packageProxy.addClass(this);
            }
        }

        public String name() {return name;}
        public String fullName() {return classInfo().getName();}
        public PackageProxy packageProxy() {return packageProxy;}
        public ClassInfo classInfo() {return classInfo;}

        public NSMutableArray names;
        public NSArray names() {
            if(names == null) {
                names = new NSMutableArray();
                ClassInfo ci = classInfo();
                FieldInfo fi[] = ci.getFields();
                for(int i = 0; i < fi.length; i++) {
                    FieldInfo f = fi[i];
                    names.addObject(f.getName());
                }
                MethodInfo mi[] = ci.getMethods();
                for(int i = 0; i < mi.length; i++) {
                    MethodInfo f = mi[i];
                    names.addObject(f.getName());
                }
                names.addObject(name());
            }
            return names;
        }
        
        public NSArray variables() {
            ClassInfo ci = classInfo();
            FieldInfo fi[] = ci.getFields();
            NSMutableArray fields = new NSMutableArray(fi.length);
            for(int i = 0; i < fi.length; i++) {
                FieldInfo f = fi[i];
                StringBuffer sb = new StringBuffer(512);
                String typeInfo = f.getType();
                String fieldName = f.getName();
                Type param = Type.tType(TypeSignature.getReturnType(typeInfo));
                sb.append(ERXStringUtilities.lastPropertyKeyInKeyPath(param.toString()));
                sb.append(" ");
                sb.append(fieldName);
                fields.addObject(sb.toString());
            }
            return fields;

        }
            
        public NSArray methods() {
            ClassInfo ci = classInfo();
            MethodInfo mi[] = ci.getMethods();
            NSMutableArray methods = new NSMutableArray(mi.length);
            for(int i = 0; i < mi.length; i++) {
                MethodInfo m = mi[i];
                StringBuffer sb = new StringBuffer(512);
                String typeInfo = m.getType();
                String params[] = TypeSignature.getParameterTypes(typeInfo);
                String methodName = m.getName();
                if(methodName.equals("<clinit>")) {
                    continue;
                }
                sb.append(Modifier.toString(m.getModifiers()));
                sb.append(" ");
                if(methodName.equals("<init>")) {
                    sb.append(ERXStringUtilities.lastPropertyKeyInKeyPath(ci.getName()));
                } else {
                    Type param = Type.tType(TypeSignature.getReturnType(typeInfo));
                    sb.append(ERXStringUtilities.lastPropertyKeyInKeyPath(param.toString()));
                    sb.append(" ");
                    sb.append(methodName);
                }
                sb.append("(");
                for(int j = 0; j < params.length; j++) {
                    Type param = Type.tType(params[j]);
                    sb.append(ERXStringUtilities.lastPropertyKeyInKeyPath(param.toString()));
                    if(j < params.length - 1)
                        sb.append(", ");
                }
                sb.append(")");
                // log.debug(sb.toString());
                methods.addObject(sb.toString());
            }
            return methods;
        }

        public String sourceCode() {
            if(sourceCode != null)
                return sourceCode;
            StringWriter writer = new StringWriter();
            try {
                Decompiler decompiler = new Decompiler();
                decompiler.setClassPath(cp);
                decompiler.decompile(classInfo().getName(), writer, null);
            } catch (IOException ex) {
                log.error(ex.toString());
            } catch (jode.AssertError ex) {
                log.error(ex.toString());
                return ex.toString() + " occurred. \n\n" 
                    + "Please check if you have all necessary jar and class files \n" 
                    + "added to ClassPaths property in the Localizable.strings file.";
            }
            sourceCode = writer.toString();
            sourceCode = JavaCCtoHTML.prettyString(sourceCode);
            sourceCode = ERXExtensions.replaceStringByStringInString("\n\n", "<br>", sourceCode);
            return sourceCode;
        }

        public String documentationPath() {
            String classPath = classInfo().getName().replace('.','/');
            for(Enumeration e = documentationPaths.objectEnumerator(); e.hasMoreElements();) {
                String path = (String)e.nextElement();
                String wholePath = path + classPath + ".html";
                File f = new File(wholePath);
                if(f.exists()) {
                    return wholePath;
                }
            }
            return null;
        }

        public String documentation() {
            String docPath = documentationPath();
            if(docPath != null) {
                documentation = ERXStringUtilities.stringWithContentsOfFile(docPath);
            } else {
                documentation = "No docs available";
            }
            return documentation;
        }

        public String toString() {return name;}

    }
}
