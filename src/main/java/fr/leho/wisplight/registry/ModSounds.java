package fr.leho.wisplight.registry;

import fr.leho.wisplight.Wisplight;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Wisplight.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_AMBIENT = register("entity.wisp.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_HURT = register("entity.wisp.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_DEATH = register("entity.wisp.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_CAPTURE = register("entity.wisp.capture");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_RELEASE = register("entity.wisp.release");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAR_ESSENCE_READY = register("block.wisp_jar.essence_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAR_HARVEST = register("block.wisp_jar.harvest");

    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_WORKING = register("block.infusion_altar.working");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_DONE = register("block.infusion_altar.done");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_SHOOT = register("item.wisp_staff.shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORB_IMPACT = register("entity.wisp_orb.impact");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Wisplight.id(name)));
    }

    private ModSounds() {}
}
