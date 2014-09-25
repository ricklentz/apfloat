package org.apfloat;

import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;

import org.apfloat.spi.Util;

/**
 * Various mathematical functions for arbitrary precision floating-point numbers.<p>
 *
 * Due to different types of round-off errors that can occur in the implementation,
 * no guarantees about e.g. monotonicity are given for any of the methods.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class ApfloatMath
{
    private ApfloatMath()
    {
    }

    /**
     * Integer power.
     *
     * @param x Base of the power operator.
     * @param n Exponent of the power operator.
     *
     * @return <code>x</code> to the <code>n</code>:th power, that is <code>x<sup>n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If both <code>x</code> and <code>n</code> are zero.
     */

    public static Apfloat pow(Apfloat x, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (n == 0)
        {
            if (x.signum() == 0)
            {
                throw new ArithmeticException("Zero to power zero");
            }

            return new Apfloat(1, Apfloat.INFINITE, x.radix());
        }
        else if (n < 0)
        {
            x = inverseRoot(x, 1);
            n = -n;
        }

        while ((n & 1) == 0)
        {
            x = x.multiply(x);
            n >>>= 1;
        }

        Apfloat r = x;

        while ((n >>>= 1) > 0)
        {
            x = x.multiply(x);
            if ((n & 1) != 0)
            {
                r = r.multiply(x);
            }
        }

        return r;
    }

    /**
     * Square root.
     *
     * @param x The argument.
     *
     * @return Square root of <code>x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> is negative.
     */

    public static Apfloat sqrt(Apfloat x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        return root(x, 2);
    }

    /**
     * Cube root.
     *
     * @param x The argument.
     *
     * @return Cube root of <code>x</code>.
     */

    public static Apfloat cbrt(Apfloat x)
        throws ApfloatRuntimeException
    {
        return root(x, 3);
    }

    /**
     * Positive integer root.
     *
     * @param x The argument.
     * @param n Which root to take.
     *
     * @return <code>n</code>:th root of <code>x</code>, that is <code>x<sup>1/n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If <code>n</code> and <code>x</code> are zero, or <code>x</code> is negative and <code>n</code> is even.
     */

    public static Apfloat root(Apfloat x, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (n == 0)
        {
            if (x.signum() == 0)
            {
                throw new ArithmeticException("Zeroth root of zero");
            }

            return new Apfloat(1, Apfloat.INFINITE, x.radix());
        }
        else if (x.signum() == 0)
        {
            return Apfloat.ZERO;                // Avoid division by zero
        }
        else if (n == 1)
        {
            return x;
        }
        else if (n == 0x8000000000000000L)
        {
            return sqrt(root(x, n / -2));
        }
        else if (n < 0)
        {
            return inverseRoot(x, -n);
        }
        else if (n == 2)
        {
            return x.multiply(inverseRoot(x, 2));
        }
        else if (n == 3)
        {
            return x.multiply(inverseRoot(x.multiply(x), 3));
        }
        else
        {
            return inverseRoot(inverseRoot(x, n), 1);
        }
    }

    /**
     * Inverse positive integer root.
     *
     * @param x The argument.
     * @param n Which inverse root to take.
     *
     * @return Inverse <code>n</code>:th root of <code>x</code>, that is <code>x<sup>-1/n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> or <code>n</code> is zero, or <code>x</code> is negative and <code>n</code> is even.
     */

    public static Apfloat inverseRoot(Apfloat x, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        return inverseRoot(x, n, x.precision());
    }

    /**
     * Inverse positive integer root.
     *
     * @param x The argument.
     * @param n Which inverse root to take.
     * @param targetPrecision Precision of the desired result.
     *
     * @return Inverse <code>n</code>:th root of <code>x</code>, that is <code>x<sup>-1/n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> or <code>n</code> is zero, or <code>x</code> is negative and <code>n</code> is even.
     */

    public static Apfloat inverseRoot(Apfloat x, long n, long targetPrecision)
        throws ArithmeticException, ApfloatRuntimeException
    {
        return inverseRoot(x, n, targetPrecision, null);
    }

    /**
     * Inverse positive integer root.
     *
     * @param x The argument.
     * @param n Which inverse root to take.
     * @param targetPrecision Precision of the desired result.
     * @param initialGuess Initial guess for the result value, or <code>null</code> if none is available.
     *
     * @return Inverse <code>n</code>:th root of <code>x</code>, that is <code>x<sup>-1/n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> or <code>n</code> is zero, or <code>x</code> is negative and <code>n</code> is even.
     */

    public static Apfloat inverseRoot(Apfloat x, long n, long targetPrecision, Apfloat initialGuess)
        throws ArithmeticException, ApfloatRuntimeException
    {
        return inverseRoot(x, n, targetPrecision, initialGuess, initialGuess == null ? 0 : initialGuess.precision());
    }

    /**
     * Inverse positive integer root.<p>
     *
     * This method is the basis for most of apfloat's non-elementary operations.
     * It is used e.g. in {@link Apfloat#divide(Apfloat)}, {@link #sqrt(Apfloat)}
     * and {@link #root(Apfloat,long)}.
     *
     * @param x The argument.
     * @param n Which inverse root to take.
     * @param targetPrecision Precision of the desired result.
     * @param initialGuess Initial guess for the result value, or <code>null</code> if none is available.
     * @param initialPrecision Precision of the initial guess, if available.
     *
     * @return Inverse <code>n</code>:th root of <code>x</code>, that is <code>x<sup>-1/n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> or <code>n</code> is zero, or <code>x</code> is negative and <code>n</code> is even.
     */

    public static Apfloat inverseRoot(Apfloat x, long n, long targetPrecision, Apfloat initialGuess, long initialPrecision)
        throws IllegalArgumentException, ArithmeticException, ApfloatRuntimeException
    {
        if (x.signum() == 0)
        {
            throw new ArithmeticException("Inverse root of zero");
        }
        else if (n == 0)
        {
            return new Apfloat(1, Apfloat.INFINITE, x.radix());
        }
        else if ((n & 1) == 0 && x.signum() < 0)
        {
            throw new ArithmeticException("Even root of negative number; result would be complex");
        }
        else if (targetPrecision <= 0)
        {
            throw new IllegalArgumentException("Target precision " + targetPrecision + " is not positive");
        }
        else if (x.equals(Apfloat.ONE))
        {
            // Trivial case
            return x.precision(targetPrecision);
        }
        else if (targetPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate inverse root to infinite precision");
        }
        else if (n == 0x8000000000000000L)
        {
            return inverseRoot(inverseRoot(x, n / -2), 2);
        }
        else if (n < 0)
        {
            return inverseRoot(inverseRoot(x, -n), 1);
        }

        long precision,
             doublePrecision = ApfloatHelper.getDoublePrecision(x.radix());
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix()),
                divisor = new Apfloat(n, Apfloat.INFINITE, x.radix()),
                result;

        if (initialGuess == null || initialPrecision < doublePrecision)
        {
            // Calculate initial guess from x
            long scaleQuot = x.scale() / n,
                 scaleRem = x.scale() - scaleQuot * n;

            result = x.precision(doublePrecision);
            result = scale(result, scaleRem - result.scale());  // Allow scales in exess of doubles'

            precision = doublePrecision;

            result = new Apfloat((double) result.signum() * Math.pow(Math.abs(result.doubleValue()), -1.0 / (double) n), precision, x.radix());
            result = scale(result, -scaleQuot);
        }
        else
        {
            // Take initial guess as given
            result = initialGuess;
            precision = initialPrecision;
        }

        int iterations = 0;

        // Compute total number of iterations
        for (long maxPrec = precision; maxPrec < targetPrecision; maxPrec <<= 1)
        {
            iterations++;
        }

        int precisingIteration = iterations;

        // Check where the precising iteration should be done
        for (long minPrec = precision; precisingIteration > 0; precisingIteration--, minPrec <<= 1)
        {
            if ((minPrec - Apfloat.EXTRA_PRECISION) << precisingIteration >= targetPrecision)
            {
                break;
            }
        }

        // Newton's iteration
        while (iterations-- > 0)
        {
            precision *= 2;
            result = result.precision(Math.min(precision, targetPrecision));

            Apfloat t = one.subtract(x.multiply(pow(result, n)));
            if (iterations < precisingIteration)
            {
                t = t.precision(precision / 2);
            }

            result = result.add(result.multiply(t).divide(divisor));

            // Precising iteration
            if (iterations == precisingIteration)
            {
                result = result.add(result.multiply(one.subtract(x.multiply(pow(result, n)))).divide(divisor));
            }
        }

        return result.precision(targetPrecision);
    }

    /**
     * Floor function. Returns the largest (closest to positive infinity) value
     * that is not greater than the argument and is equal to a mathematical integer.
     *
     * @param x The argument.
     *
     * @return <code>x</code> rounded towards negative infinity.
     */

    public static Apint floor(Apfloat x)
        throws ApfloatRuntimeException
    {
        return x.floor();
    }

    /**
     * Ceiling function. Returns the smallest (closest to negative infinity) value
     * that is not less than the argument and is equal to a mathematical integer.
     *
     * @param x The argument.
     *
     * @return <code>x</code> rounded towards positive infinity.
     */

    public static Apint ceil(Apfloat x)
        throws ApfloatRuntimeException
    {
        return x.ceil();
    }

    /**
     * Truncates fractional part.
     *
     * @param x The argument.
     *
     * @return <code>x</code> rounded towards zero.
     */

    public static Apint truncate(Apfloat x)
        throws ApfloatRuntimeException
    {
        return x.truncate();
    }

    /**
     * Returns an apfloat whose value is <code>-x</code>.
     *
     * @param x The argument.
     *
     * @return <code>-x</code>.
     */

    public static Apfloat negate(Apfloat x)
        throws ApfloatRuntimeException
    {
        return Apfloat.ZERO.subtract(x);
    }

    /**
     * Absolute value.
     *
     * @param x The argument.
     *
     * @return Absolute value of <code>x</code>.
     */

    public static Apfloat abs(Apfloat x)
        throws ApfloatRuntimeException
    {
        if (x.signum() >= 0)
        {
            return x;
        }
        else
        {
            return negate(x);
        }
    }

    /**
     * Multiply by a power of the radix.
     *
     * @param x The argument.
     * @param scale The scaling factor.
     *
     * @return <code>x*x.radix()<sup>scale</sup></code>.
     */

    public static Apfloat scale(Apfloat x, long scale)
        throws ApfloatRuntimeException
    {
        if (scale == 0 || x.signum() == 0)
        {
            return x;
        }

        Apfloat radix = new Apfloat(x.radix(), Apfloat.INFINITE, x.radix()),
                result;

        if ((scale & 0xC000000000000000L) != 0)
        {
            // The exponent might overflow in the string or in intermediate calculations
            Apfloat scaler1 = pow(radix, Math.abs(scale) >>> 1),
                    scaler2 = ((scale & 1) == 0 ? scaler1 : scaler1.multiply(radix));
            result = (scale >= 0 ? x.multiply(scaler1).multiply(scaler2) : x.divide(scaler1).divide(scaler2));
        }
        else if (x.radix() <= 14)
        {
            Apfloat scaler = new Apfloat("1e" + scale, Apfloat.INFINITE, x.radix());
            result = x.multiply(scaler);
        }
        else
        {
            // "e" would be interpreted as a digit
            Apfloat scaler = pow(radix, Math.abs(scale));
            result = (scale >= 0 ? x.multiply(scaler) : x.divide(scaler));
        }

        return result;
    }

    /**
     * Split to integer and fractional parts.
     * The integer part is simply <code>i = floor(x)</code>.
     * For the fractional part <code>f</code> the following is always true:<p>
     *
     * <code>0 <= f < 1</code>
     *
     * @param x The argument.
     *
     * @return An array of two apfloats, <code>[i, f]</code>, the first being the integer part and the last being the fractional part.
     */

    public static Apfloat[] modf(Apfloat x)
        throws ApfloatRuntimeException
    {
        Apfloat[] result = new Apfloat[2];

        result[0] = floor(x);
        result[1] = x.subtract(result[0]);

        return result;
    }

    /**
     * Returns x modulo y.<p>
     *
     * This function calculates the remainder <code>f</code> of <code>x / y</code>
     * such that <code>x = i * y + f</code>, where <code>i</code> is an integer,
     * <code>f</code> has the same sign as <code>x</code>,
     * and the absolute value of <code>f</code> is less than the absolute value of <code>y</code>.<p>
     *
     * If <code>y</code> is zero, then zero is returned.
     *
     * @param x The dividend.
     * @param y The divisor.
     *
     * @return The remainder when x is divided by y.
     */

    public static Apfloat fmod(Apfloat x, Apfloat y)
        throws ApfloatRuntimeException
    {
        if (y.signum() == 0)
        {
            return y;                           // By definition
        }
        else if (x.signum() == 0)
        {
            // 0 % x = 0
            return x;
        }

        long precision;
        Apfloat t, a, b, tx, ty;

        a = abs(x);
        b = abs(y);

        if (a.compareTo(b) < 0)
        {
            return x;                           // abs(x) < abs(y)
        }
        else if (x.precision() <= x.scale() - y.scale())
        {
            return Apfloat.ZERO;                // Degenerate case; not enough precision to make any sense
        }
        else
        {
            precision = x.scale() - y.scale() + Apfloat.EXTRA_PRECISION;        // Some extra precision to avoid round-off errors
        }

        tx = x.precision(precision);
        ty = y.precision(precision);

        t = tx.divide(ty).truncate();           // Approximate division

        precision = Math.min(Util.ifFinite(y.precision(), y.precision() + x.scale() - y.scale()), x.precision());

        tx = x.precision(precision);
        ty = y.precision(precision);

        t = tx.subtract(t.multiply(ty));

        a = abs(t);

        if (a.compareTo(b) >= 0)                // Fix division round-off error
        {
            t = new Apfloat(x.signum(), Apfloat.INFINITE, x.radix()).multiply(a.subtract(b));
        }

        return t;
    }

    /**
     * Fused multiply-add. Calculates <code>a * b + c * d</code>
     * so that the precision used in the multiplications is only
     * what is needed for the end result. Performance can this way
     * be better than by calculating <code>a.multiply(b).add(c.multiply(d))</code>.
     *
     * @param a First argument.
     * @param b Second argument.
     * @param c Third argument.
     * @param d Fourth argument.
     *
     * @return <code>a * b + c * d</code>.
     */

    public static Apfloat multiplyAdd(Apfloat a, Apfloat b, Apfloat c, Apfloat d)
    {
        return multiplyAddOrSubtract(a, b, c, d, false);
    }

    /**
     * Fused multiply-subtract. Calculates <code>a * b - c * d</code>
     * so that the precision used in the multiplications is only
     * what is needed for the end result. Performance can this way
     * be better than by calculating <code>a.multiply(b).subtract(c.multiply(d))</code>.
     *
     * @param a First argument.
     * @param b Second argument.
     * @param c Third argument.
     * @param d Fourth argument.
     *
     * @return <code>a * b - c * d</code>.
     */

    public static Apfloat multiplySubtract(Apfloat a, Apfloat b, Apfloat c, Apfloat d)
    {
        return multiplyAddOrSubtract(a, b, c, d, true);
    }

    private static Apfloat multiplyAddOrSubtract(Apfloat a, Apfloat b, Apfloat c, Apfloat d, boolean subtract)
    {
        long[] precisions;
        Apfloat ab, cd;

        precisions = ApfloatHelper.getMatchingPrecisions(a, b, c, d);
        if (precisions[0] == 0)
        {
            ab = Apfloat.ZERO;
        }
        else
        {
            a = a.precision(precisions[0]);
            b = b.precision(precisions[0]);
            ab = a.multiply(b);
        }
        if (precisions[1] == 0)
        {
            cd = Apfloat.ZERO;
        }
        else
        {
            c = c.precision(precisions[1]);
            d = d.precision(precisions[1]);
            cd = c.multiply(d);
        }

        Apfloat result = (subtract ? ab.subtract(cd) : ab.add(cd));

        return (result.signum() == 0 ? result : result.precision(precisions[2]));
    }

    /**
     * Arithmetic-geometric mean.
     *
     * @param a First argument.
     * @param b Second argument.
     *
     * @return Arithmetic-geometric mean of a and b.
     */

    public static Apfloat agm(Apfloat a, Apfloat b)
        throws ApfloatRuntimeException
    {
        if (a.signum() == 0 || b.signum() == 0)         // Would not converge quadratically
        {
            return Apfloat.ZERO;
        }

        long workingPrecision = Math.min(a.precision(), b.precision()),
             targetPrecision = Math.max(a.precision(), b.precision());

        if (workingPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate agm to infinite precision");
        }

        // Some extra precision is required for the algorithm to work accurately
        workingPrecision = ApfloatHelper.extendPrecision(workingPrecision);
        a = ApfloatHelper.ensurePrecision(a, workingPrecision);
        b = ApfloatHelper.ensurePrecision(b, workingPrecision);

        long precision = 0,
             halfWorkingPrecision = (workingPrecision + 1) / 2;
        final long CONVERGING = 1000;           // Arbitrarily chosen value...
        Apfloat two = new Apfloat(2, Apfloat.INFINITE, a.radix());

        // First check convergence
        while (precision < CONVERGING && precision < halfWorkingPrecision)
        {
            Apfloat t = a.add(b).divide(two);
            b = sqrt(a.multiply(b));
            a = t;

            // Conserve precision in case of accumulating round-off errors
            a = ApfloatHelper.ensurePrecision(a, workingPrecision);
            b = ApfloatHelper.ensurePrecision(b, workingPrecision);

            precision = a.equalDigits(b);
        }

        // Now we know quadratic convergence
        while (precision <= halfWorkingPrecision)
        {
            Apfloat t = a.add(b).divide(two);
            b = sqrt(a.multiply(b));
            a = t;

            // Conserve precision in case of accumulating round-off errors
            a = ApfloatHelper.ensurePrecision(a, workingPrecision);
            b = ApfloatHelper.ensurePrecision(b, workingPrecision);

            precision *= 2;
        }

        return a.add(b).divide(two).precision(targetPrecision);
    }

    /**
     * Calculates &pi;. Uses default radix.
     *
     * @param precision Number of digits of &pi; to calculate.
     *
     * @return &pi; accurate to <code>precision</code> digits, in the default radix.
     *
     * @exception java.lang.NumberFormatException If the default radix is not valid.
     * @exception java.lang.IllegalArgumentException In case the precision is invalid.
     */

    public static Apfloat pi(long precision)
        throws IllegalArgumentException, NumberFormatException, ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        int radix = ctx.getDefaultRadix();

        return pi(precision, radix);
    }

    /**
     * Calculates &pi;.
     *
     * @param precision Number of digits of &pi; to calculate.
     * @param radix The radix in which the number should be presented.
     *
     * @return &pi; accurate to <code>precision</code> digits, in base <code>radix</code>.
     *
     * @exception java.lang.NumberFormatException If the radix is not valid.
     * @exception java.lang.IllegalArgumentException In case the precision is invalid.
     */

    public static Apfloat pi(long precision, int radix)
        throws IllegalArgumentException, NumberFormatException, ApfloatRuntimeException
    {
        if (precision <= 0)
        {
            throw new IllegalArgumentException("Precision " + precision + " is not positive");
        }
        else if (precision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate pi to infinite precision");
        }

        // Get synchronization lock - getting the lock is also synchronized
        Integer radixKey = getRadixPiKey(new Integer(radix));

        Apfloat pi;

        // Synchronize getting pre-calculated pi by radix key
        // - two threads won't try to calculate it at the same time
        // - doesn't block getting it for other radixes
        synchronized (radixKey)
        {
            pi = (Apfloat) ApfloatMath.radixPi.get(radixKey);

            if (pi == null || pi.precision() < precision)
            {
                pi = calculatePi(precision, radixKey);
            }
            else
            {
                pi = pi.precision(precision);
            }
        }

        return pi;
    }

    // Get shared radix key for synchronizing getting and calculating the pi related constants
    // This method is also synchronized to ensure that the lock is unique for each radix at all times
    private static synchronized Integer getRadixPiKey(Integer radix)
    {
        Integer radixKey = (Integer) ApfloatMath.radixPiKeys.get(radix);
        if (radixKey == null)
        {
            ApfloatMath.radixPiKeys.put(radix, radix);
            radixKey = radix;
        }

        return radixKey;
    }

    /**
     * Simple JavaBean to hold one apfloat. This class can
     * also be thought of as a pointer to an apfloat.
     */

    private static class ApfloatHolder
    {
        public ApfloatHolder()
        {
            this(null);
        }

        public ApfloatHolder(Apfloat apfloat)
        {
            this.apfloat = apfloat;
        }

        public Apfloat getApfloat()
        {
            return this.apfloat;
        }

        public void setApfloat(Apfloat apfloat)
        {
            this.apfloat = apfloat;
        }

        private Apfloat apfloat;
    }

    private static class PiCalculator
    {
        public PiCalculator(int radix)
        {
            this.A = new Apfloat(13591409, Apfloat.INFINITE, radix);
            this.B = new Apfloat(545140134, Apfloat.INFINITE, radix);
            this.J = new Apfloat(10939058860032000L, Apfloat.INFINITE, radix);
            this.ONE = new Apfloat(1, Apfloat.INFINITE, radix);
            this.TWO = new Apfloat(2, Apfloat.INFINITE, radix);
            this.FIVE = new Apfloat(5, Apfloat.INFINITE, radix);
            this.SIX = new Apfloat(6, Apfloat.INFINITE, radix);
            this.radix = radix;
        }

        private Apfloat a(long n)
            throws ApfloatRuntimeException
        {
            Apfloat s = new Apfloat(n, Apfloat.INFINITE, this.radix),
                    v = this.A.add(this.B.multiply(s));

            v = ((n & 1) == 0 ? v : negate(v));

            return v;
        }

        private Apfloat p(long n)
            throws ApfloatRuntimeException
        {
            Apfloat v;

            if (n == 0)
            {
                v = this.ONE;
            }
            else
            {
                Apfloat f = new Apfloat(n, Apfloat.INFINITE, this.radix),
                        sixf = this.SIX.multiply(f);

                v = sixf.subtract(this.ONE).multiply(this.TWO.multiply(f).subtract(this.ONE)).multiply(sixf.subtract(this.FIVE));
            }

            return v;
        }

        private Apfloat q(long n)
            throws ApfloatRuntimeException
        {
            Apfloat v;

            if (n == 0)
            {
                v = this.ONE;
            }
            else
            {
                Apfloat f = new Apfloat(n, Apfloat.INFINITE, this.radix);

                v = this.J.multiply(f).multiply(f).multiply(f);
            }

            return v;
        }

        public void r(long n1, long n2, ApfloatHolder T, ApfloatHolder Q, ApfloatHolder P)
            throws ApfloatRuntimeException
        {
            int length = (int) Math.min(n2 - n1, Integer.MAX_VALUE);

            switch (length)             // Java can't switch on a long...
            {
                case 0:
                {
                    assert (n1 != n2);

                    break;
                }
                case 1:
                {
                    Apfloat p0 = p(n1);

                    T.setApfloat(a(n1).multiply(p0));
                    Q.setApfloat(q(n1));
                    P.setApfloat(p0);

                    break;
                }
                case 2:
                {
                    Apfloat p0 = p(n1), p01 = p0.multiply(p(n1 + 1)),
                            q1 = q(n1 + 1);

                    T.setApfloat(q1.multiply(a(n1)).multiply(p0).add(
                                 a(n1 + 1).multiply(p01)));
                    Q.setApfloat(q(n1).multiply(q1));
                    P.setApfloat(p01);

                    break;
                }
                case 3:
                {
                    Apfloat p0 = p(n1), p01 = p0.multiply(p(n1 + 1)), p012 = p01.multiply(p(n1 + 2)),
                            q2 = q(n1 + 2), q12 = q(n1 + 1).multiply(q2);

                    T.setApfloat(q12.multiply(a(n1)).multiply(p0).add(
                         q2.multiply(a(n1 + 1)).multiply(p01)).add(
                         a(n1 + 2).multiply(p012)));
                    Q.setApfloat(q(n1).multiply(q12));
                    P.setApfloat(p012);

                    break;
                }
                case 4:
                {
                    Apfloat p0 = p(n1), p01 = p0.multiply(p(n1 + 1)), p012 = p01.multiply(p(n1 + 2)), p0123 = p012.multiply(p(n1 + 3)),
                            q3 = q(n1 + 3), q23 = q(n1 + 2).multiply(q3), q123 = q(n1 + 1).multiply(q23);

                    T.setApfloat(q123.multiply(a(n1)).multiply(p0).add(
                         q23.multiply(a(n1 + 1)).multiply(p01)).add(
                         q3.multiply(a(n1 + 2)).multiply(p012)).add(
                         a(n1 + 3).multiply(p0123)));
                    Q.setApfloat(q(n1).multiply(q123));
                    P.setApfloat(p0123);

                    break;
                }
                default:
                {
                    long nMiddle = (n1 + n2) / 2;
                    ApfloatHolder LT = new ApfloatHolder(),
                                  LQ = new ApfloatHolder(),
                                  LP = new ApfloatHolder();

                    r(n1, nMiddle, LT, LQ, LP);
                    r(nMiddle, n2, T, Q, P);

                    T.setApfloat(Q.getApfloat().multiply(LT.getApfloat()).add(LP.getApfloat().multiply(T.getApfloat())));
                    Q.setApfloat(LQ.getApfloat().multiply(Q.getApfloat()));
                    P.setApfloat(LP.getApfloat().multiply(P.getApfloat()));
                }
            }
        }

        private final Apfloat A;
        private final Apfloat B;
        private final Apfloat J;
        private final Apfloat ONE;
        private final Apfloat TWO;
        private final Apfloat FIVE;
        private final Apfloat SIX;
        private int radix;
    }

    // Perform actual calculation of pi for radix, and store the result to pre-calulation maps.
    // Uses the Chudnovskys' binary splitting algorithm.
    // Uses previously calculated terms (if such exist) to improve the precision of the calculation.
    private static Apfloat calculatePi(long precision, Integer radixKey)
        throws ApfloatRuntimeException
    {
        int radix = radixKey.intValue();
        PiCalculator piCalculator = (PiCalculator) ApfloatMath.radixPiCalculator.get(radixKey);
        if (piCalculator == null)
        {
            piCalculator = new PiCalculator(radix);
            ApfloatMath.radixPiCalculator.put(radixKey, piCalculator);
        }

        Apfloat LT,
                LQ,
                LP,
                inverseRoot;

        ApfloatHolder RT = new ApfloatHolder(),
                      RQ = new ApfloatHolder(),
                      RP = new ApfloatHolder();

        // Perform the calculation of T, Q and P to infinite precision
        // to make possible to use them later for further calculations

        long neededTerms = (long) ((double) precision * Math.log((double) radix) / 32.65445004177),
             workingPrecision = ApfloatHelper.extendPrecision(precision);   // To avoid cumulative round-off errors

        Long terms = (Long) ApfloatMath.radixPiTerms.get(radixKey);
        if (terms != null)
        {
            // Some terms have been calculated already previously
            long currentTerms = terms.longValue();

            LT = (Apfloat) ApfloatMath.radixPiT.get(radixKey);
            LQ = (Apfloat) ApfloatMath.radixPiQ.get(radixKey);
            LP = (Apfloat) ApfloatMath.radixPiP.get(radixKey);

            // Check if there actually are more needed terms or if the needed
            // extra precision is just a few digits achievable with current terms
            if (currentTerms != neededTerms + 1)
            {
                piCalculator.r(currentTerms, neededTerms + 1, RT, RQ, RP);

                LT = RQ.getApfloat().multiply(LT).add(LP.multiply(RT.getApfloat()));
                LQ = LQ.multiply(RQ.getApfloat());
                LP = LP.multiply(RP.getApfloat());
            }

            // Improve the inverse root value from the current precision
            inverseRoot = (Apfloat) ApfloatMath.radixPiInverseRoot.get(radixKey);
            inverseRoot = inverseRoot(new Apfloat(640320, workingPrecision, radix), 2, workingPrecision, inverseRoot);
        }
        else
        {
            piCalculator.r(0, neededTerms + 1, RT, RQ, RP);

            LT = RT.getApfloat();
            LQ = RQ.getApfloat();
            LP = RP.getApfloat();

            inverseRoot = inverseRoot(new Apfloat(640320, workingPrecision, radix), 2);
        }

        Apfloat pi = inverseRoot(inverseRoot.multiply(LT), 1).multiply(new Apfloat(53360, Apfloat.INFINITE, radix)).multiply(LQ);

        // Limit precisions to actual after extended working precisions
        inverseRoot = inverseRoot.precision(precision);
        pi = pi.precision(precision);

        // Put the updated values to the hashtable
        ApfloatMath.radixPiT.put(radixKey, LT);
        ApfloatMath.radixPiQ.put(radixKey, LQ);
        ApfloatMath.radixPiP.put(radixKey, LP);
        ApfloatMath.radixPiInverseRoot.put(radixKey, inverseRoot);
        ApfloatMath.radixPiTerms.put(radixKey, new Long(neededTerms + 1));
        ApfloatMath.radixPi.put(radixKey, pi);

        return pi;
    }

    /**
     * Natural logarithm.<p>
     *
     * The logarithm is calculated using the arithmetic-geometric mean.
     * See the Borweins' book for the formula.
     *
     * @param x The argument.
     *
     * @return Natural logarithm of <code>x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>x <= 0</code>.
     */

    public static Apfloat log(Apfloat x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (x.signum() <= 0)
        {
            throw new ArithmeticException("Logarithm of " + (x.signum() == 0 ? "zero" : "negative number; result would be complex"));
        }
        else if (x.equals(Apfloat.ONE))
        {
            return Apfloat.ZERO;
        }

        // Calculate the log using 1 / radix <= x < 1 and the log addition formula
        // because the agm converges badly for big x.

        long targetPrecision = x.precision();
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix());
        long finalPrecision = Util.ifFinite(targetPrecision, targetPrecision - one.equalDigits(x));     // If the argument is close to 1, the result is less accurate

        long originalScale = x.scale();

        x = scale(x, -originalScale);   // Set x's scale to zero

        Apfloat radixPower;
        if (originalScale == 0)
        {
            radixPower = Apfloat.ZERO;
        }
        else
        {
            Apfloat logRadix = ApfloatHelper.extendPrecision(logRadix(targetPrecision, x.radix()));
            radixPower = new Apfloat(originalScale, Apfloat.INFINITE, x.radix()).multiply(logRadix);
        }

        return ApfloatHelper.extendPrecision(rawLog(x)).add(radixPower).precision(finalPrecision);
    }

    // Raw logarithm, regardless of x
    // Doesn't work for big x, but is faster if used alone for small numbers
    private static Apfloat rawLog(Apfloat x)
        throws ApfloatRuntimeException
    {
        assert (x.signum() > 0);                                        // Must be real logarithm

        long targetPrecision = x.precision();

        if (targetPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate logarithm to infinite precision");
        }

        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix());

        final int EXTRA_PRECISION = 25;

        long workingPrecision = ApfloatHelper.extendPrecision(targetPrecision),
             n = targetPrecision / 2 + EXTRA_PRECISION;                 // Very rough estimate

        x = ApfloatHelper.extendPrecision(x, EXTRA_PRECISION);

        Apfloat e = one.precision(workingPrecision);
        e = scale(e, -n);
        x = scale(x, -n);

        Apfloat agme = ApfloatHelper.extendPrecision(agm(one, e)),
                agmex = ApfloatHelper.extendPrecision(agm(one, x));

        Apfloat pi = ApfloatHelper.extendPrecision(pi(targetPrecision, x.radix())),
                log = pi.multiply(agmex.subtract(agme)).divide(new Apfloat(2, Apfloat.INFINITE, x.radix()).multiply(agme).multiply(agmex));

        return log.precision(targetPrecision);
    }

    // Get shared radix key for synchronizing getting and calculating the logarithm related constants
    // This method is also synchronized to ensure that the lock is unique for each radix at all times
    private static synchronized Integer getRadixLogKey(Integer radix)
    {
        Integer radixKey = (Integer) ApfloatMath.radixLogKeys.get(radix);
        if (radixKey == null)
        {
            ApfloatMath.radixLogKeys.put(radix, radix);
            radixKey = radix;
        }

        return radixKey;
    }

    /**
     * Gets or calculates logarithm of a radix to required precision.
     * The calculated value is stored in a cache for later usage.
     *
     * @param precision The needed precision.
     * @param radix The radix.
     *
     * @return Natural logarithm of <code>radix</code> to the specified precision.
     *
     * @exception java.lang.NumberFormatException If the radix is invalid.
     */

    public static Apfloat logRadix(long precision, int radix)
        throws ApfloatRuntimeException
    {
        // Get synchronization lock - getting the lock is also synchronized
        Integer radixKey = getRadixLogKey(new Integer(radix));

        Apfloat logRadix;

        // Synchronize getting pre-calculated log by radix key
        // - two threads won't try to calculate it at the same time
        // - doesn't block getting it for other radixes
        synchronized (radixKey)
        {
            logRadix = (Apfloat) ApfloatMath.radixLog.get(radixKey);

            if (logRadix == null || logRadix.precision() < precision)
            {
                Apfloat f = new Apfloat("0.1", precision, radix);

                logRadix = negate(rawLog(f));

                ApfloatMath.radixLog.put(radixKey, logRadix);
            }
            else
            {
                logRadix = logRadix.precision(precision);
            }
        }

        return logRadix;
    }

    /**
     * Exponent function.
     * Calculated using Newton's iteration for the inverse of logarithm.
     *
     * @param x The argument.
     *
     * @return <code>e<sup>x</sup></code>.
     */

    public static Apfloat exp(Apfloat x)
        throws ApfloatRuntimeException
    {
        int radix = x.radix();

        if (x.signum() == 0)
        {
            return new Apfloat(1, Apfloat.INFINITE, radix);
        }

        long targetPrecision = x.precision(),
             precision,
             doublePrecision = ApfloatHelper.getDoublePrecision(radix);

        // If the argument is close to 0, the result is more accurate
        targetPrecision = Util.ifFinite(targetPrecision, targetPrecision + Math.max(1 - x.scale(), 0));

        if (targetPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate exponent to infinite precision");
        }
        else if (x.compareTo(new Apfloat((double) Long.MAX_VALUE * Math.log((double) radix), doublePrecision, radix)) >= 0)
        {
            throw new ApfloatRuntimeException("Overflow");
        }
        else if (x.scale() <= Long.MIN_VALUE / 2 + Apfloat.EXTRA_PRECISION)
        {
            // Taylor series: exp(x) = 1 + x + x^2/2 + ...

            return new Apfloat(1, Apfloat.INFINITE, radix).add(x);
        }

        Apfloat result;

        if (x.scale() < -doublePrecision / 2)
        {
            // Taylor series: exp(x) = 1 + x + x^2/2 + ...

            precision = -2 * x.scale();
            result = new Apfloat(1, precision, radix).add(x);
        }
        else
        {
            // Approximate starting value for iteration

            // An overflow should not occur
            double doubleValue = x.doubleValue() / Math.log((double) radix),
                   integerPart = Math.floor(doubleValue),
                   fractionalPart = doubleValue - integerPart;

            result = new Apfloat(Math.pow((double) radix, fractionalPart), doublePrecision, radix);
            result = scale(result, (long) integerPart);

            // Initial precision is reduced if x is very big
            int integerPartDigits = (integerPart > 0 ? (int) Math.floor(Math.log(integerPart + 0.5) / Math.log((double) radix)) : 0);
            precision = Math.max(1, doublePrecision - integerPartDigits);
        }

        // Precalculate the needed values once to the required precision
        logRadix(targetPrecision, radix);

        int iterations = 0;

        // Compute total number of iterations
        for (long maxPrec = precision; maxPrec < targetPrecision; maxPrec <<= 1)
        {
            iterations++;
        }

        int precisingIteration = iterations;

        // Check where the precising iteration should be done
        for (long minPrec = precision; precisingIteration > 0; precisingIteration--, minPrec <<= 1)
        {
            if ((minPrec - Apfloat.EXTRA_PRECISION) << precisingIteration >= targetPrecision)
            {
                break;
            }
        }

        x = ApfloatHelper.extendPrecision(x);

        // Newton's iteration
        while (iterations-- > 0)
        {
            precision *= 2;
            result = result.precision(Math.min(precision, targetPrecision));

            Apfloat t = log(result);
            t = lastIterationExtendPrecision(iterations, precisingIteration, t);
            t = x.subtract(t);

            if (iterations < precisingIteration)
            {
                t = t.precision(precision / 2);
            }

            result = lastIterationExtendPrecision(iterations, precisingIteration, result);
            result = result.add(result.multiply(t));

            // Precising iteration
            if (iterations == precisingIteration)
            {
                t = log(result);
                t = lastIterationExtendPrecision(iterations, -1, t);

                result = lastIterationExtendPrecision(iterations, -1, result);
                result = result.add(result.multiply(x.subtract(t)));
            }
        }

        return result.precision(targetPrecision);
    }

    // Extend the precision on last iteration
    private static Apfloat lastIterationExtendPrecision(int iterations, int precisingIteration, Apfloat x)
    {
        return (iterations == 0 && precisingIteration != 0 ? ApfloatHelper.extendPrecision(x) : x);
    }

    /**
     * Arbitrary power. Calculated using <code>log()</code> and <code>exp()</code>.<p>
     *
     * This method doesn't calculate the result properly if <code>x</code> is negative
     * and <code>y</code> is an integer. For that you should use {@link #pow(Apfloat,long)}.
     *
     * @param x The base.
     * @param y The exponent.
     *
     * @return <code>x<sup>y</sup></code>.
     *
     * @exception java.lang.ArithmeticException If both <code>x</code> and <code>y</code> are zero, or <code>x</code> is negative.
     */

    public static Apfloat pow(Apfloat x, Apfloat y)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (y.signum() == 0)
        {
            if (x.signum() == 0)
            {
                throw new ArithmeticException("Zero to power zero");
            }

            return new Apfloat(1, Apfloat.INFINITE, x.radix());
        }
        else if (x.signum() == 0 || x.equals(Apfloat.ONE) || y.equals(Apfloat.ONE))
        {
            return x;
        }
        else if (x.signum() < 0)
        {
            throw new ArithmeticException("Power of negative number; result would be complex");
        }

        long targetPrecision = Math.min(x.precision(), y.precision());

        // Try to precalculate the needed values just once to the required precision,
        // this may not work too efficiently if x is close to 1 or y is close to zero
        logRadix(targetPrecision, x.radix());

        // Limits precision for log() but may be sub-optimal; precision could be limited more
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix());
        targetPrecision = Util.ifFinite(targetPrecision, targetPrecision + one.equalDigits(x)); // If the log() argument is close to 1, the result is less accurate
        x = x.precision(Math.min(x.precision(), targetPrecision));

        Apfloat result = log(x);
        long intermediatePrecision = Math.min(y.precision(), result.precision());
        result = ApfloatHelper.extendPrecision(result);
        result = ApfloatHelper.extendPrecision(y).multiply(result);
        result = exp(result.precision(intermediatePrecision));

        return result;
    }

    /**
     * Inverse hyperbolic cosine. Calculated using <code>log()</code>.
     *
     * @param x The argument.
     *
     * @return Inverse hyperbolic cosine of <code>x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>x <= 1</code>.
     */

    public static Apfloat acosh(Apfloat x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix());

        if (x.signum() >= 0)
        {
            return log(x.add(sqrt(x.multiply(x).subtract(one))));
        }
        else
        {
            return negate(log(x.subtract(sqrt(x.multiply(x).subtract(one)))));
        }
    }

    /**
     * Inverse hyperbolic sine. Calculated using <code>log()</code>.
     *
     * @param x The argument.
     *
     * @return Inverse hyperbolic sine of <code>x</code>.
     */

    public static Apfloat asinh(Apfloat x)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix());

        if (x.signum() >= 0)
        {
            return log(sqrt(x.multiply(x).add(one)).add(x));
        }
        else
        {
            return negate(log(sqrt(x.multiply(x).add(one)).subtract(x)));
        }
    }

    /**
     * Inverse hyperbolic tangent. Calculated using <code>log()</code>.
     *
     * @param x The argument.
     *
     * @return Inverse hyperbolic tangent of <code>x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>abs(x) >= 1</code>.
     */

    public static Apfloat atanh(Apfloat x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, x.radix());

        return log(one.add(x).divide(one.subtract(x))).divide(two);
    }

    /**
     * Hyperbolic cosine. Calculated using <code>exp()</code>.
     *
     * @param x The argument.
     *
     * @return Hyperbolic cosine of <code>x</code>.
     */

    public static Apfloat cosh(Apfloat x)
        throws ApfloatRuntimeException
    {
        Apfloat y = exp(x),
                one = new Apfloat(1, Apfloat.INFINITE, x.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, x.radix());

        return y.add(one.divide(y)).divide(two);
    }

    /**
     * Hyperbolic sine. Calculated using <code>exp()</code>.
     *
     * @param x The argument.
     *
     * @return Hyperbolic sine of <code>x</code>.
     */

    public static Apfloat sinh(Apfloat x)
        throws ApfloatRuntimeException
    {
        Apfloat y = exp(x),
                one = new Apfloat(1, Apfloat.INFINITE, x.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, x.radix());

        return y.subtract(one.divide(y)).divide(two);
    }

    /**
     * Hyperbolic tangent. Calculated using <code>exp()</code>.
     *
     * @param x The argument.
     *
     * @return Hyperbolic tangent of <code>x</code>.
     */

    public static Apfloat tanh(Apfloat x)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, x.radix()),
                y = exp(two.multiply(abs(x)));

        y = y.subtract(one).divide(y.add(one));
        return (x.signum() < 0 ? negate(y) : y);
    }

    /**
     * Inverse cosine. Calculated using complex functions.
     *
     * @param x The argument.
     *
     * @return Inverse cosine of <code>x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>abs(x) > 1</code>.
     */

    public static Apfloat acos(Apfloat x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one);

        return ApcomplexMath.log(x.add(i.multiply(sqrt(one.subtract(x.multiply(x)))))).imag();
    }

    /**
     * Inverse sine. Calculated using complex functions.
     *
     * @param x The argument.
     *
     * @return Inverse sine of <code>x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>abs(x) > 1</code>.
     */

    public static Apfloat asin(Apfloat x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one);

        return negate(ApcomplexMath.log(sqrt(one.subtract(x.multiply(x))).subtract(i.multiply(x))).imag());
    }

    /**
     * Inverse tangent. Calculated using complex functions.
     *
     * @param x The argument.
     *
     * @return Inverse tangent of <code>x</code>.
     */

    public static Apfloat atan(Apfloat x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, x.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, x.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one);

        return ApcomplexMath.log(i.subtract(x).divide(i.add(x))).imag().divide(two);
    }

    /**
     * Converts cartesian coordinates to polar coordinates. Calculated using complex functions.<p>
     *
     * Computes the phase angle by computing an arc tangent of <code>x/y</code> in the range of -&pi; < angle <= &pi;.
     *
     * @param x The argument.
     * @param y The argument.
     *
     * @return The angle of the point <code>(y, x)</code> in the plane.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> and <code>y</code> are both zero.
     */

    public static Apfloat atan2(Apfloat x, Apfloat y)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (y.signum() == 0)
        {
            if (x.signum() == 0)
            {
                throw new ArithmeticException("Angle of (0, 0)");
            }

            Apfloat pi = pi(x.precision(), x.radix()),
                    two = new Apfloat(2, Apfloat.INFINITE, x.radix());

            return new Apfloat(x.signum(), Apfloat.INFINITE, x.radix()).multiply(pi).divide(two);
        }
        else if (x.signum() == 0)
        {
            if (y.signum() > 0)
            {
                return Apfloat.ZERO;
            }

            return pi(y.precision(), y.radix());
        }
        else if (Math.min(x.precision(), y.precision()) == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate atan2 to infinite precision");
        }
        else
        {
            long maxScale = Math.max(x.scale(), y.scale());

            x = scale(x, -maxScale);    // Now neither x nor y is zero
            y = scale(y, -maxScale);

            return ApcomplexMath.log(new Apcomplex(y, x)).imag();
        }
    }

    /**
     * Cosine. Calculated using complex functions.
     *
     * @param x The argument.
     *
     * @return Cosine of <code>x</code>.
     */

    public static Apfloat cos(Apfloat x)
        throws ApfloatRuntimeException
    {
        return ApcomplexMath.exp(new Apcomplex(Apfloat.ZERO, x)).real();
    }

    /**
     * Sine. Calculated using complex functions.
     *
     * @param x The argument.
     *
     * @return Sine of <code>x</code>.
     */

    public static Apfloat sin(Apfloat x)
        throws ApfloatRuntimeException
    {
        return ApcomplexMath.exp(new Apcomplex(Apfloat.ZERO, x)).imag();
    }

    /**
     * Tangent. Calculated using complex functions.
     *
     * @param x The argument.
     *
     * @return Tangent of <code>x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> is &pi;/2 + n &pi; where n is an integer.
     */

    public static Apfloat tan(Apfloat x)
        throws ApfloatRuntimeException
    {
        Apcomplex w = ApcomplexMath.exp(new Apcomplex(Apfloat.ZERO, x));

        return w.imag().divide(w.real());
    }

    // Synchronization keys for pi calculation
    private static Map radixPiKeys = new HashMap();

    // Shared precalculated values related to pi for different radixes
    private static Map radixPi = new Hashtable();
    private static Map radixPiCalculator = new Hashtable();
    private static Map radixPiT = new Hashtable();
    private static Map radixPiQ = new Hashtable();
    private static Map radixPiP = new Hashtable();
    private static Map radixPiInverseRoot = new Hashtable();
    private static Map radixPiTerms = new Hashtable();

    // Synchronization keys for logarithm calculation
    private static Map radixLogKeys = new HashMap();

    // Shared precalculated values related to logarithm for different radixes
    private static Map radixLog = new Hashtable();
}
