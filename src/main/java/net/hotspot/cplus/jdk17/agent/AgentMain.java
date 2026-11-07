package net.hotspot.cplus.jdk17.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentMain {

    public static Instrumentation instrumentation;

    public static int tick = 0;

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("agent load by agentmain");
        instrumentation = inst;
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                System.err.println("agent: " + className );
                tick++;
                return null;
            }
        });

    }

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("agent load by premain");
        instrumentation = inst;
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                System.err.println("agent");
                tick++;
                return null;
            }
        });
    }
}
