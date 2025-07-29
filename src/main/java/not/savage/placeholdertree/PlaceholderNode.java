package not.savage.placeholdertree;

public interface PlaceholderNode<T> {

    /**
     * Resolve the expected value for the final placeholder node.
     * @param context the context in which the placeholder is being resolved, provided as parent's context type.
     * @return the resolved value for the placeholder node, or an empty string if no value is available.
     */
    String resolve(T context);

}
