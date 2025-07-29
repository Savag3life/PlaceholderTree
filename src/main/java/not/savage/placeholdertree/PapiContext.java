package not.savage.placeholdertree;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.OfflinePlayer;

/**
 * Default context passed when used Via PlaceholderAPI.
 */
@AllArgsConstructor
@Data
public class PapiContext implements Context {

    private String[] args;
    private OfflinePlayer offlinePlayer;

    @Override
    public String[] getArguments() {
        return args;
    }

    @Override
    public OfflinePlayer player() {
        return offlinePlayer;
    }
}
