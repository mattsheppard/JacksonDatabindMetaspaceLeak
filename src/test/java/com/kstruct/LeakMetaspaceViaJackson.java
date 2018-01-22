package com.kstruct;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

public class LeakMetaspaceViaJackson {

    @Test
    public void test() throws IOException, ResourceException, ScriptException, InstantiationException, IllegalAccessException, InterruptedException {
        File groovyScriptFile = new File("src/test/resources/groovy/Foo.groovy");
        GroovyScriptEngine engine = new GroovyScriptEngine(groovyScriptFile.getParentFile().getAbsolutePath());
        ObjectMapper mapper = new ObjectMapper();
        
        while(true) {
            Thread.sleep(500);
            // We touch the file so Groovy's script engine will
            // create a new class loader and load it as a new class.
            groovyScriptFile.setLastModified(System.currentTimeMillis());

            mapper.getTypeFactory().clearCache();
            
            Class groovyClass = engine.loadScriptByName(groovyScriptFile.getName());

            Object groovyObject = groovyClass.newInstance();
            
            System.out.println(mapper.writeValueAsString(groovyObject));

            // Note that we haven't kept any reference to the groovyClass or
            // groovyObject, so they should be collectable in both the heap
            // and meta-space.

            // https://github.com/FasterXML/jackson-databind/issues/489
            // suggests we may be expected to call this if we're dynamically
            // creating types
            mapper.getTypeFactory().clearCache();
            
            System.out.println(((DefaultSerializerProvider) mapper.getSerializerProvider()).cachedSerializersCount());
            ((DefaultSerializerProvider) mapper.getSerializerProvider()).flushCachedSerializers();
        }
    }
}
