package dev.vibecoderqin.lunarspoof.transform;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Changes the client-brand string and invokes the public channel handshake after Join Game.
 * It does not emulate the Lunar launcher or protected-server anticheat.
 */
public final class BrandAndChannelTransformer implements IClassTransformer {
    private static final String TARGET_CLASS = "net.minecraft.client.network.NetHandlerPlayClient";
    private static final String BRAND_OWNER = "net/minecraft/client/ClientBrandRetriever";
    private static final String HELPER_OWNER = "dev/vibecoderqin/lunarspoof/LunarSpoofCore";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);
        boolean changed = false;

        for (MethodNode method : classNode.methods) {
            boolean foundBrandCall = false;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; ) {
                AbstractInsnNode next = instruction.getNext();
                if (instruction instanceof MethodInsnNode && isClientBrandCall((MethodInsnNode) instruction)) {
                    method.instructions.set(instruction, new LdcInsnNode("vanilla"));
                    foundBrandCall = true;
                }
                instruction = next;
            }

            if (foundBrandCall) {
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; ) {
                    AbstractInsnNode next = instruction.getNext();
                    if (instruction.getOpcode() == Opcodes.RETURN) {
                        InsnList hook = new InsnList();
                        hook.add(new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                HELPER_OWNER,
                                "afterJoinGame",
                                "()V",
                                false
                        ));
                        method.instructions.insertBefore(instruction, hook);
                    }
                    instruction = next;
                }
                changed = true;
                break;
            }
        }

        if (!changed) {
            return basicClass;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isClientBrandCall(MethodInsnNode call) {
        if (call.getOpcode() != Opcodes.INVOKESTATIC || !"()Ljava/lang/String;".equals(call.desc)) {
            return false;
        }
        return BRAND_OWNER.equals(call.owner)
                || call.owner.endsWith("/ClientBrandRetriever")
                || "getClientModName".equals(call.name);
    }
}
