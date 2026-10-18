package uz.orbitalstrike;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class Msg {
    private Msg() {}

    static Component ok(String s) { return Component.text(s, NamedTextColor.GREEN); }
    static Component err(String s) { return Component.text(s, NamedTextColor.RED); }
    static Component info(String s) { return Component.text(s, NamedTextColor.YELLOW); }
    static Component plain(String s) { return Component.text(s, NamedTextColor.GRAY); }
}
