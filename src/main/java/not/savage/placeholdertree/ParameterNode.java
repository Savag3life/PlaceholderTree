package not.savage.placeholdertree;

@FunctionalInterface
public interface ParameterNode<P extends Context, T extends Context> {

    /**
     *
     * @param context Context passed via parent node (possibly previously transformed)
     * @return Transformed context
     * @throws InvalidParamException Thrown if the context is invalid for this node
     */
    T transform(P context) throws InvalidParamException;

}
