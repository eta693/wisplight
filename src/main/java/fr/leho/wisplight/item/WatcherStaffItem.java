package fr.leho.wisplight.item;

/** Bâton du Veilleur : trois orbes à la fois, 8 dégâts chacun. */
public class WatcherStaffItem extends WispStaffItem {
    public WatcherStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    protected int shots() {
        return 3;
    }

    @Override
    protected float damageBonus() {
        return 4.0F;
    }
}
