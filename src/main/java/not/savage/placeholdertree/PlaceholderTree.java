package not.savage.placeholdertree;


import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

public class PlaceholderTree {

    public static final String CATCH_ALL_CHAR = "*";
    @Getter private final Branch<PapiContext, PapiContext> root;

    private PlaceholderTree(Branch<PapiContext, PapiContext> root) {
        this.root = root;
    }

    public String resolve(PapiContext context) {
        return root.resolve(context);
    }

    public void printAllNodes(Logger logger) {
        printAllNodesRecursive(root, root.getPath(), logger);
    }

    private void printAllNodesRecursive(Branch<?, ?> branch, String currentPath, Logger logger) {
        if (branch == null) return;

        // Print all nodes in this branch
        for (var entry : branch.getNodes().entrySet()) {
            logger.info("%s_%s".formatted(currentPath, entry.getKey()));
        }

        // Recursively print child branches
        for (var entry : branch.getChildren().entrySet()) {
            String childKey = entry.getKey();
            Branch<?, ?> childBranch = entry.getValue();
            printAllNodesRecursive(childBranch, currentPath + "_" + childKey, logger);
        }
    }


    /**
     * Create a new placeholder tree builder instance
     * @param path The root path of this tree. Should be
     * @param contextTransformer Transform the default PapiContext at the root level.
     * @return A new builder instance for this tree.
     * @param <T> This node's context type
     * @param <P> Parent node's context type
     */
    public static <T extends Context, P extends Builder<PapiContext, ? extends Context, ?>> Builder<T, PapiContext, P> builder(String path, Function<PapiContext, T> contextTransformer) {
        return new Builder<>(path, contextTransformer);
    }

    /**
     * Create a new placeholder tree builder instance with the default PapiContext
     * @param path The root path of this tree. Should be
     * @return A new builder instance for this tree.
     * @param <P> Parent node's context type
     */
    public static <P extends Builder<PapiContext, ? extends Context, ?>> Builder<PapiContext, PapiContext, P> builder(String path) {
        return new Builder<>(path, Function.identity());
    }

    /**
     * Any given node in a placeholder tree. A Node may have child nodes, or placeholder nodes.
     * Each `_` within a placeholder indicates a new node in the tree.
     * @param <T> This node's context type
     * @param <P> Parent node's context type
     */
    @Data
    public static class Branch<T extends Context, P extends Context> {
        private final Branch<P, ?> parent;
        private final String path;
        private final Function<P, T> contextTransformer;
        private final Map<String, Branch<?, T>> children;
        private final Map<String, PlaceholderNode<T>> nodes;
        private final ParameterNode<P, T> parameterNode;

        protected Branch(String path, Branch<P, ?> parent, Function<P, T> contextTransformer,
                         Map<String, Branch<?, T>> children, Map<String, PlaceholderNode<T>> nodes,
                         ParameterNode<P, T> parameterNode) {
            this.path = path;
            this.parent = parent;
            this.contextTransformer = contextTransformer;
            this.children = children;
            this.nodes = nodes;
            this.parameterNode = parameterNode;
        }

        /**
         * Attempt to resolve a given placeholder path against this node & its children.
         * @param context the context to resolve against this node
         * @return the resolved string, or an empty string if no match was found.
         */
        public String resolve(P context) {
            if (context.getArguments().length == 0 || context.getArguments()[0].isEmpty()) return "";
            if (!context.getArguments()[0].equals(path)) return null;
            final T transformedContext = contextTransformer.apply(context);
            return resolveRecursive(1, transformedContext); // idx 1 because "root" is in index 0 and that is this class. Handled via PlaceholderTree.resolve()
        }

        public String resolve(int idx, P context) {
            T transformedContext = contextTransformer.apply(context);
            return resolveRecursive(idx, transformedContext);
        }

        @SuppressWarnings("unchecked")
        private String resolveRecursive(int idx, T context) {
            if (idx + 1 >= context.getArguments().length) { // This is the end of the path.
                final PlaceholderNode<T> node = nodes.get(context.getArguments()[idx]);
                if (node != null) {
                    return node.resolve(context);
                }
                return "";
            }

            final Branch<?, T> child = children.get(context.getArguments()[idx]);
            if (child == null) return "";

            return child.resolve(idx + 1, context);
        }
    }

    /**
     * Builder for creating a placeholder tree.
     * @param <T> This node's context type
     * @param <P> Parent node's context type
     * @param <B> Parent builder type
     */
    @Getter
    public static class Builder<T extends Context, P extends Context, B extends Builder<P, ? extends Context, ?>> {
        private final String path;
        private final B parent;
        private final Function<P, T> contextTransformer;
        private final Map<String, Builder<?, T, Builder<T, P, B>>> children = new HashMap<>();
        private final HashMap<String, PlaceholderNode<T>> node = new HashMap<>();
        private ParameterNode<P, T> parameterNode;
        private String paramIdentifier;

        private Builder(B parent, String path, Function<P, T> contextTransformer) {
            this.parent = parent;
            this.path = path;
            this.contextTransformer = contextTransformer;
        }

        private Builder(String path, Function<P, T> contextTransformer) {
            this.parent = null;
            this.path = path;
            this.contextTransformer = contextTransformer;
        }

        /**
         * Get the parent builder of this builder.
         * @return the parent builder
         */
        public B parent() {
            if (parent == null)
                throw new IllegalStateException("This is the root builder, it has no parent.");
            return parent;
        }

        /**
         * Create a new placeholder group (node/branch) within our current group.
         * @param key The key for this group, used to identify it in the tree.
         * @param contextTransformer Function to transform the parent context into a child context.
         * @return A new builder instance for the child group.
         * @param <C> The context type for the child group
         */
        public <C extends Context> Builder<C, T, Builder<T, P, B>> group(String key, Function<T, C> contextTransformer) {
            final Builder<C, T, Builder<T, P, B>> childBuilder = new Builder<>(this, key, contextTransformer);
            children.put(key, childBuilder);
            return childBuilder;
        }

        /**
         * Create a new placeholder group (node/branch) within our current group.
         * @param key The key for this group, used to identify it in the tree.
         * @param childBuilder The builder instance for the child group.
         * @return A new builder instance for the child group.
         */
        public Builder<T, P, B> merge(String key, Builder<?, T, Builder<T, P, B>> childBuilder) {
            if (key == null || key.isEmpty())
                throw new IllegalArgumentException("Key cannot be null or empty.");

            if (key.equals(CATCH_ALL_CHAR))
                throw new IllegalArgumentException("Key cannot be '*' as it is reserved for catch-all nodes.");

            if (children.containsKey(key))
                throw new IllegalArgumentException("Group with key '" + key + "' already exists in the builder.");

            children.put(key, childBuilder);
            return this;
        }


        /**
         * Create a new placeholder node within our current group/node/branch.
         * @param key The key for this group, used to identify it in the tree.
         * @return A new builder instance for the child group.
         */
        public Builder<T, P, B> node(String key, PlaceholderNode<T> replacer) {
            if (key == null || key.isEmpty())
                throw new IllegalArgumentException("Key cannot be null or empty.");

            if (key.equals(CATCH_ALL_CHAR))
                throw new IllegalArgumentException("Key cannot be '*' as it is reserved for catch-all nodes.");

            if (node.containsKey(key))
                throw new IllegalArgumentException("Node with key '" + key + "' already exists in the builder.");

            node.put(key, replacer);
            return this;
        }

        /**
         * If this group contains a "catch all", and if no node matches the given path, the catch-all node will be used.
         * This is useful for creating id or in-path params that can be used to match any path.
         * Only one catch-all node can exist in a given group.
         * example_group_<my_group_id>_name -> placeholder group "group" has node "count" and catch-all node.
         * example_group_count -> matches to node "count" in the group.
         * @param catchAllNode The catch-all node to use if no other node matches the given path.
         * @return this builder instance
         */
        public Builder<T, P, B> catchAll(CatchAllNode<T> catchAllNode) {
            if (node.containsKey(CATCH_ALL_CHAR))
                throw new IllegalArgumentException("Catch-all node already exists in the builder.");

            node.put(CATCH_ALL_CHAR, catchAllNode);
            return this;
        }

        public Builder<T, P, B> param(String identifier, ParameterNode<P, T> transformer) {
            if (identifier == null || identifier.isEmpty())
                throw new IllegalArgumentException("Identifier cannot be null or empty.");

            if (identifier.equals(CATCH_ALL_CHAR))
                throw new IllegalArgumentException("Identifier cannot be '*' as it is reserved for catch-all nodes.");

            if (node.containsKey(identifier))
                throw new IllegalArgumentException("Node with identifier '" + identifier + "' already exists in the builder.");

            parameterNode = transformer;
            return this;
        }

        protected Branch<T, P> buildNode(Branch<P, ?> parent) {
            final Branch<T, P> newNode = new Branch<>(path, parent, contextTransformer, new HashMap<>(), new HashMap<>(), parameterNode);
            for (Map.Entry<String, Builder<?, T, Builder<T, P, B>>> entry : children.entrySet()) {
                String childKey = entry.getKey();
                Builder<?, T, Builder<T, P, B>> childBuilder = entry.getValue();
                Branch<?, T> childNode = childBuilder.buildNode(newNode);
                newNode.getChildren().put(childKey, childNode);
            }
            for (Map.Entry<String, PlaceholderNode<T>> entry : this.node.entrySet()) {
                String nodeKey = entry.getKey();
                PlaceholderNode<T> placeholderNode = entry.getValue();
                newNode.getNodes().put(nodeKey, placeholderNode);
            }
            return newNode;
        }

        /**
         * Build a new placeholder tree from this builder.
         * @return A new PlaceholderTree instance with the root node built from this builder.
         */
        @SuppressWarnings("unchecked")
        public PlaceholderTree build() {
            if (parent != null) return parent().build();

            Branch<T, P> rootNode = new Branch<>(path, null, contextTransformer, new HashMap<>(), new HashMap<>(), parameterNode);
            for (Map.Entry<String, Builder<?, T, Builder<T, P, B>>> entry : children.entrySet()) {
                String childKey = entry.getKey();
                Builder<?, T, Builder<T, P, B>> childBuilder = entry.getValue();
                Branch<?, T> childNode = childBuilder.buildNode(rootNode);
                rootNode.getChildren().put(childKey, childNode);
            }
            for (Map.Entry<String, PlaceholderNode<T>> entry : this.node.entrySet()) {
                String nodeKey = entry.getKey();
                PlaceholderNode<T> placeholderNode = entry.getValue();
                rootNode.getNodes().put(nodeKey, placeholderNode);
            }
            return new PlaceholderTree((Branch<PapiContext, PapiContext>) rootNode);
        }
    }
}

