package fr.leho.wisplight.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

/** Appels réservés au client (jamais exécutés sur un serveur dédié). */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openGrimoire(int pages) {
        List<Component> content = new ArrayList<>();
        for (int i = 1; i <= pages; i++) {
            content.add(Component.translatable("book.wisplight.grimoire.page" + i));
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(content)));
    }
}
