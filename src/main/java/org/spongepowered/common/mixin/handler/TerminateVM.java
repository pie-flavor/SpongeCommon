/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.handler;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.common.mixin.handler.TerminateVM.MasqueradeClassLoader;
import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * Really wish this wasn't necessary but unfortunately FML doesn't have any
 * mechanism to shut down the VM when a fatal error occurs.
 */
public final class TerminateVM implements IExitHandler {
    
    static class MasqueradeClassLoader extends ClassLoader {

        final LaunchClassLoader parent;
        final String className, classRef;

        MasqueradeClassLoader(final LaunchClassLoader parent, final String masqueradePackage) {
            super(parent);
            this.parent = parent;
            this.className = masqueradePackage + ".TerminateVM";
            this.classRef = this.className.replace('.', '/');
        }
        
        String getClassName() {
            return this.className;
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            try {
                if (this.className.equals(name)) {
                    final ClassWriter cw = new ClassWriter(0);
                    final ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
                        @Override
                        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
                            super.visit(version, access | Opcodes.ACC_PUBLIC, MasqueradeClassLoader.this.classRef, signature, superName, interfaces);
                        }
                        
                        @Override
                        public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                            if ("terminate".equals(name)) {
                                return null;
                            }
                            final MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
                            return new MethodVisitor(Opcodes.ASM5, mv) {
                                @Override
                                public void visitMethodInsn(final int opcode, String owner, String name, final String desc, final boolean itf) {
                                    if (owner.endsWith("TerminateVM")) {
                                        owner = MasqueradeClassLoader.this.classRef;
                                        if ("systemExit".equals(name)) {
                                            owner = System.class.getName().replace('.', '/');
                                            name = "exit";
                                        }
                                    }
                                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                                }
                            };
                        }
                    };
                    new ClassReader(this.parent.getClassBytes(TerminateVM.class.getName())).accept(cv, 0);
                    final byte[] classBytes = cw.toByteArray();
                    return this.defineClass(this.className, classBytes, 0, classBytes.length, null);
                }
            } catch (IOException ex) {
                // sad face
            }
            
            return this.parent.findClass(name);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void terminate(final String masqueradePackage, final int status) {
        final Logger log = LogManager.getLogger("Sponge");

        IExitHandler handler = null;
        try {
            final MasqueradeClassLoader cl = new MasqueradeClassLoader(Launch.classLoader, masqueradePackage);
            final Constructor<IExitHandler> ctor = ((Class<IExitHandler>) Class.forName(cl.getClassName(), true, cl)).getDeclaredConstructor();
            ctor.setAccessible(true);
            handler = ctor.newInstance();
        } catch (Throwable th) {
            log.catching(th);
            handler = new TerminateVM();
        }
        handler.exit(status);
    }

    @Override
    public void exit(final int status) {
        TerminateVM.systemExit(status);
    }

    private static void systemExit(final int status) {
        throw new IllegalStateException("Not transformed");
    }
}
