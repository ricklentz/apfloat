package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ArrayAccess;

/**
 * Array access class based on a <code>double[]</code>.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class DoubleMemoryArrayAccess
    extends ArrayAccess
{
    /**
     * Create an array access.<p>
     *
     * @param data The underlying array.
     * @param offset The offset of the access segment within the array.
     * @param length The access segment.
     */

    public DoubleMemoryArrayAccess(double[] data, int offset, int length)
    {
        super(offset, length);
        this.data = data;
    }

    public ArrayAccess subsequence(int offset, int length)
    {
        return new DoubleMemoryArrayAccess(this.data, getOffset() + offset, length);
    }

    public Object getData()
    {
        return this.data;
    }

    public double[] getDoubleData()
    {
        return this.data;
    }

    public void close()
        throws ApfloatRuntimeException
    {
        this.data = null;       // Might have an impact on garbage collection
    }

    private double[] data;
}
