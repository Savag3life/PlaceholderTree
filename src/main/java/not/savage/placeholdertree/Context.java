package not.savage.placeholdertree;

import org.bukkit.OfflinePlayer;

/**
 * This current implementation uses a `Context` build around PlaceholderAPI's
 * arguments provided by PlaceholderExpansion methods.
 */
public interface Context {

    /**
     * String arguments, resolved through `param.split("_")`
     * @return String array of arguments
     */
    String[] getArguments();

    /**
     * The player this context is for.
     * @return The player
     */
    OfflinePlayer player();

}
