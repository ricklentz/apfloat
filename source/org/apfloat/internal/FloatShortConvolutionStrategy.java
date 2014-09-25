package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ConvolutionStrategy;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;
import static org.apfloat.internal.FloatRadixConstants.*;

/**
 * Short convolution strategy.
 * Performs a simple multiplication when the size of one operand is 1.
 *
 * @version 1.1
 * @author Mikko Tommila
 */

public class FloatShortConvolutionStrategy
    extends FloatBaseMath
    implements ConvolutionStrategy
{
    /**
     * Creates a convolution strategy using the specified radix.
     *
     * @param radix The radix that will be used.
     */

    public FloatShortConvolutionStrategy(int radix)
    {
        super(radix);
    }

    public DataStorage convolute(DataStorage x, DataStorage y, long resultSize)
        throws ApfloatRuntimeException
    {
        DataStorage shortStorage, longStorage, resultStorage;

        if (x.getSize() > 1)
        {
            shortStorage = y;
            longStorage = x;
        }
        else
        {
            shortStorage = x;
            longStorage = y;
        }

        assert (shortStorage.getSize() == 1);

        long size = longStorage.getSize() + 1;

        ArrayAccess arrayAccess = shortStorage.getArray(DataStorage.READ, 0, 1);
        float factor = arrayAccess.getFloatData()[arrayAccess.getOffset()];
        arrayAccess.close();

        ApfloatContext ctx = ApfloatContext.getContext();
        DataStorageBuilder dataStorageBuilder = ctx.getBuilderFactory().getDataStorageBuilder();
        resultStorage = dataStorageBuilder.createDataStorage(size * 4);
        resultStorage.setSize(size);

        DataStorage.Iterator src = longStorage.iterator(DataStorage.READ, size - 1, 0),
                             dst = resultStorage.iterator(DataStorage.WRITE, size, 0);

        float carry = baseMultiplyAdd(src, null, factor, 0, dst, size - 1);
        dst.setFloat(carry);
        dst.close();

        return resultStorage;
    }

    private static final long serialVersionUID = 3839614758362699756L;
}
