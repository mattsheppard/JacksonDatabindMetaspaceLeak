package com.kstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

import groovy.util.GroovyScriptEngine;

public class LeakMetaspaceViaJackson {

    private static final String GROOVY_SCRIPT_NAME_PREFIX = "Script";
    private static final int NUM_GROOVY_SCRIPTS = 1000;

    public static void main(String[] args) throws Exception {
        leak();
    }
    
    public static void leak() throws Exception {
        File groovyClassDir = new File("target/generated_groovy_classes");
        groovyClassDir.mkdirs();
        
        regenerateGroovyScripts(groovyClassDir);
        
        GroovyScriptEngine engine = new GroovyScriptEngine(groovyClassDir.getAbsolutePath());
        ObjectMapper mapper = new ObjectMapper();
        
        while(true) {
            regenerateGroovyScripts(groovyClassDir);

            for (int i = 0; i < NUM_GROOVY_SCRIPTS; i++) {
                String groovyScriptName = GROOVY_SCRIPT_NAME_PREFIX + i + ".groovy";

                Class groovyClass = engine.loadScriptByName(groovyScriptName);

                Object groovyObject = groovyClass.newInstance();
                
//                String json = mapper.writeValueAsString(groovyObject);
//                System.out.println(json);
            }

            // Note that we haven't kept any reference to any groovyClasses or
            // groovyObjects, so they should be collectable in both the heap
            // and meta-space.

            // https://github.com/FasterXML/jackson-databind/issues/489
            // suggests we may be expected to call this if we're dynamically
            // creating types
            mapper.getTypeFactory().clearCache();
            
            // If found that clearing this helps a bit (but doesn't totally fix the problem)
//            System.out.println(((DefaultSerializerProvider) mapper.getSerializerProvider()).cachedSerializersCount());
            ((DefaultSerializerProvider) mapper.getSerializerProvider()).flushCachedSerializers();
        }
    }

    /**
     * Generate 1000 groovy script files which GroovyScriptEngine will reload (because the
     * modification time changed).
     */
    private static void regenerateGroovyScripts(File groovyClassDir) throws IOException {
        for (int i = 0; i < NUM_GROOVY_SCRIPTS; i++) {
            File groovyScriptFile = new File(groovyClassDir, GROOVY_SCRIPT_NAME_PREFIX + i + ".groovy");
            byte[] groovyScriptContent = ("public class Foo { public String name = \"NUMBER-" + i + "\"; }").getBytes();
            Files.write(groovyScriptFile.toPath(), groovyScriptContent);
        }
    }
}
