# Placeholder Tree's
Placeholder Tree offers a more maintainable way to create and manage [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) placeholders—and potentially any placeholder system. It allows you to group placeholders into logical sets, while transforming the provided `Context` as needed for each placeholder.
## Examples
```java
PlaceholderTree.builder("prisons", Function.identity())
        .group("stats", context -> {
            return new PrisonsContext(...);
        })
        .node("blocks", prisonContext -> {
            return prisonContext.getPrisonPlayer().getTotalBlocksMined();
        })
        .parent()
        .group("balance", context -> {
            return new BalanceContext(...);
        })
        .node("btc", balanceContext -> {
            return balanceContext.getPrisonPlayer().getBalance(Currency.BITCOIN);
        })
        .node("eth", balanceContext -> {
            return balanceContext.getPrisonPlayer().getBalance(Currency.ETHEREUM);
        })
        .node("usd", balanceContext -> {
            return balanceContext.getPrisonPlayer().getBalance(Currency.DOLLAR);
        })
        .build();
```
## Basics
Each tree acts essentially as a linked list. Each node contains a `PlaceholderNode` set, which hold this nodes complete placeholders. Each node may also contain multiple child nodes. Each node or branch also has the ability to transform the inbound context using the previous context. This allows placeholders to share data instead of each method needing to retrieve data on its own.
### `node(pathSuffix, parseFunction)`
Add a new placeholder to the current node's set. A `node` is the end of the line for resolving placeholders. It is a full, complete path to retrieve data.
### `group(pathSuffix, contextTransformer)`
Add a new child node to the current node. This allows you to create a new branch in the tree, which can then have its own placeholders. The `contextTransformer` is used to transform the context from the parent node into a new context for this group. If this node, and it's parent share a context, `Function.identity()` should be used.
### `parent()`
Return to the parent node. This allows you to create a new branch in the tree, while still being able to return to the previous node.
### `build()`
Build the tree. This will return a `PlaceholderTree` instance, which can then be used to register placeholders with PlaceholderAPI. Can be called at any level, the root node will be resolved automatically.
### `catchAll()` *beta*
Allows you to create a "catch all" or `*` node. If no `PlaceholderNode` within the current branch matches the requested placeholder, this node will be used instead. This is useful for creating id or parameter fields within placeholders, similar to `factions_<faction_id>_members` where `<faction_id>` is a catch-all node, which we would attempt to resolve ourselves on each request.

## Planned Features
- [ ] Refactor to separate Minecraft related code & the standalone library.
- [ ] Add a timed caching system, to allow faster resolving and caching results to reduce load.