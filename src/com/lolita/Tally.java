package com.lolita;

/**
 * Interface required for use in general Reduce and Scan classes.
 *
 * @param <ElemType>  data element type being tallied
 */
public interface Tally <ElemType>{
    /**
     * This method acts as a factory to create new Tally objects of the same type.
     * Typically, these have the same instance data as <code>this</code> except for
     * identity-like values. Used for the start of a reduce or scan operation.
     *
     * @return  a new identity Tally
     */
    public Tally<ElemType> init();

    /**
     * This method clones the Tally such that it is independent of <code>this</code>.
     *
     * @return  a new object that has the same data
     */
    public Tally<ElemType> clone();

    /**
     * Accumulate an data element into this Tally.
     *
     * @param elem   data element to accumulate into the Tally
     */
    public void accum(ElemType elem);

    /**
     * Combing this Tally with another.
     * We assume that the the other is the exact same type and size.
     * Used to combine subtrees in a reduction or scan.
     *
     * @param other  the right side of the reduction evaluation tree
     */
    public void combine(Tally<ElemType> other);

    /**
     * Occasionally, we'd like to create a Tally directly from a single data element.
     *
     * @param elem  single element to include in the returned Tally
     * @return      Tally with just the given elem's contribution
     */
    public default Tally<ElemType> prepare(ElemType elem) {
        Tally<ElemType> tally = init();
        tally.accum(elem);
        return tally;
    }
}
