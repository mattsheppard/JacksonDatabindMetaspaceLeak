# JacksonDatabindMetaspaceLeak

Demonstration project for reporting a case where Jackson seems to cause a JVM metaspace leak

Try running it with...

```
> mvn clean dependency:copy-dependencies package
> java -XX:MaxMetaspaceSize=20m -cp "target/dependency/*:target/jackson-classolader-leaker-0.0.1-SNAPSHOT.jar" com.kstruct.LeakMetaspaceViaJackson
```

On my machine it runs happily for about a minute, then GC goes a bit crazy, loads of heap is allocated (but not used) and eventually we start getting bunches of out of memory errors.

I'll update this with a link to the Jackson issue once I've raised it.
