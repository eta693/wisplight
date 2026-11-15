package fr.leho.wisplight.gametest;

import fr.leho.wisplight.Wisplight;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Wisplight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SmokeTests {
    private SmokeTests() {}

    @GameTest(template = "empty")
    public static void modLoads(GameTestHelper helper) {
        helper.succeed();
    }
}
