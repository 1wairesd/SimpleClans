package net.sacredlabyrinth.phaed.simpleclans.hooks.papi.resolvers;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.hooks.papi.PlaceholderResolver;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Map;

@SuppressWarnings("unused")
public class MethodReturnResolver extends PlaceholderResolver {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.##");

    public MethodReturnResolver(@NotNull SimpleClans plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String getId() {
        return "method_return";
    }

    @Override
    public @NotNull String resolve(@Nullable OfflinePlayer player, @NotNull Object object, @NotNull Method method,
                                   @NotNull String placeholder, @NotNull Map<String, String> config) {
        Object result = invoke(object, method, placeholder);
        if (result == null) {
            return "";
        }
        if (result instanceof Boolean) {
            return ((Boolean) result) ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        }
        // Форматируем double значения правильно (без научной нотации)
        if (result instanceof Double || result instanceof Float) {
            return DECIMAL_FORMAT.format(((Number) result).doubleValue());
        }
        return String.valueOf(result);
    }
}
