package net.sacredlabyrinth.phaed.simpleclans.hooks.papi.resolvers;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.hooks.papi.PlaceholderResolver;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class RankingPositionResolver extends PlaceholderResolver {
    public RankingPositionResolver(@NotNull SimpleClans plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String getId() {
        return "ranking_position";
    }

    @Override
    public @NotNull String resolve(@Nullable OfflinePlayer player, @NotNull Object object, @NotNull Method method,
                                   @NotNull String placeholder, @NotNull Map<String, String> config) {
        if (object instanceof Clan) {
            List<Clan> clans = plugin.getClanManager().getClans();
            
            // Определяем тип сортировки по имени плейсхолдера
            if (placeholder.contains("kills")) {
                plugin.getClanManager().sortClansByKills(clans);
            } else if (placeholder.contains("balance")) {
                plugin.getClanManager().sortClansByBalance(clans);
            } else {
                // По умолчанию сортируем по KDR
                plugin.getClanManager().sortClansByKDR(clans);
            }

            int position = clans.indexOf(object) + 1;
            return position > 0 ? position + "." : "";
        }
        if (object instanceof ClanPlayer) {
            List<ClanPlayer> clanPlayers = plugin.getClanManager().getAllClanPlayers();
            
            // Определяем тип сортировки по имени плейсхолдера
            if (placeholder.contains("kills")) {
                plugin.getClanManager().sortClanPlayersByKills(clanPlayers);
            } else {
                // По умолчанию сортируем по KDR
                plugin.getClanManager().sortClanPlayersByKDR(clanPlayers);
            }

            int position = clanPlayers.indexOf(object) + 1;
            return position > 0 ? position + "." : "";
        }

        return "";
    }
}
