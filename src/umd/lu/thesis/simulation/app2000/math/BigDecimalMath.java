package umd.lu.thesis.simulation.app2000.math;

import java.security.*;
import java.util.*;
import java.math.*;

/**
 * BigDecimal special functions.
 * <a href="http://arxiv.org/abs/0908.3030">A Java Math.BigDecimal
 * Implementation of Core Mathematical Functions</a>
 *
 * @since 2009-05-22
 * @author Richard J. Mathar
 * @see <a href="http://apfloat.org/">apfloat</a>
 * @see <a href="http://dfp.sourceforge.net/">dfp</a>
 * @see <a href="http://jscience.org/">JScience</a>
 */
public class BigDecimalMath {

    /**
     * The base of the natural logarithm in a predefined accuracy.
     * http://www.cs.arizona.edu/icon/oddsends/e.htm The precision of the
     * predefined constant is one less than the string's length, taking into
     * account the decimal dot. static int E_PRECISION = E.length()-1 ;
     */
    final static BigDecimal E = new BigDecimal("2.71828");

    /**
     * A suggestion for the maximum numter of terms in the Taylor expansion of
     * the exponential.
     */
    final static private int TAYLOR_NTERM = 8;

    /**
     * The integer root.
     *
     * @param n the positive argument.
     * @param x the non-negative argument.
     * @return The n-th root of the BigDecimal rounded to the precision implied
     * by x, x^(1/n).
     */
    private static BigDecimal root(final int n, final BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            throw new ArithmeticException("negative argument " + x.toString() + " of root");
        }
        if (n <= 0) {
            throw new ArithmeticException("negative power " + n + " of root");
        }
        if (n == 1) {
            return x;
        }
        /* start the computation from a double precision estimate */
        BigDecimal s = new BigDecimal(Math.pow(x.doubleValue(), 1.0 / n));
        /* this creates nth with nominal precision of 1 digit
         */
        final BigDecimal nth = new BigDecimal(n);
        /* Specify an internal accuracy within the loop which is
         * slightly larger than what is demanded by ’eps’ below.
         */
        final BigDecimal xhighpr = scalePrec(x, 2);
        MathContext mc = new MathContext(2 + x.precision());
        /* Relative accuracy of the result is eps.
         */
        final double eps = x.ulp().doubleValue() / (2 * n * x.doubleValue());
        for (;;) {
            /* s = s -(s/n-x/n/s^(n-1)) = s-(s-x/s^(n-1))/n; test correction s/n-x/s for being
             * smaller than the precision requested. The relative correction is (1-x/s^n)/n,
             */
            BigDecimal c = xhighpr.divide(s.pow(n - 1), mc);
            c = s.subtract(c);
            MathContext locmc = new MathContext(c.precision());
            c = c.divide(nth, locmc);
            s = s.subtract(c);
            if (Math.abs(c.doubleValue() / s.doubleValue()) < eps) {
                break;
            }
        }
        return s.round(new MathContext(err2prec(eps)));
    } /* BigDecimalMath.root */


    /**
     * The exponential function.
     *
     * @param x the argument.
     * @return exp(x). The precision of the result is implicitly defined by the
     * precision in the argument. 16 In particular this means that "Invalid
     * Operation" errors are thrown if catastrophic cancellation of digits
     * causes the result to have no valid digits left.
     */
    public static BigDecimal exp(BigDecimal x) {
        /* To calculate the value if x is negative, use exp(-x) = 1/exp(x)
         */
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            final BigDecimal invx = exp(x.negate());
            /* Relative error in inverse of invx is the same as the relative errror in invx.
             * This is used to define the precision of the result.
             */
            MathContext mc = new MathContext(invx.precision());
            return BigDecimal.ONE.divide(invx, mc);
        } else if (x.compareTo(BigDecimal.ZERO) == 0) {
            /* recover the valid number of digits from x.ulp(), if x hits the
             * zero. The x.precision() is 1 then, and does not provide this information.
             */
            return scalePrec(BigDecimal.ONE, -(int) (Math.log10(x.ulp().doubleValue())));
        } else {
            /* Push the number in the Taylor expansion down to a small
             * value where TAYLOR_NTERM terms will do. If x<1, the n-th term is of the order
             * x^n/n!, and equal to both the absolute and relative error of the result
             * since the result is close to 1. The x.ulp() sets the relative and absolute error
             * of the result, as estimated from the first Taylor term.
             * We want x^TAYLOR_NTERM/TAYLOR_NTERM! < x.ulp, which is guaranteed if
             * x^TAYLOR_NTERM < TAYLOR_NTERM*(TAYLOR_NTERM-1)*...*x.ulp.
             */
            final double xDbl = x.doubleValue();
            final double xUlpDbl = x.ulp().doubleValue();
            if (Math.pow(xDbl, TAYLOR_NTERM) < TAYLOR_NTERM * (TAYLOR_NTERM - 1.0) * (TAYLOR_NTERM - 2.0) * xUlpDbl) {
                /* Add TAYLOR_NTERM terms of the Taylor expansion (Euler’s sum formula)
                 */
                BigDecimal resul = BigDecimal.ONE;
                /* x^i */
                BigDecimal xpowi = BigDecimal.ONE;
                /* i factorial */
                BigInteger ifac = BigInteger.ONE;
                /* TAYLOR_NTERM terms to be added means we move x.ulp() to the right
                 * for each power of 10 in TAYLOR_NTERM, so the addition won’t add noise beyond
                 * what’s already in x.
                 */
                MathContext mcTay = new MathContext(err2prec(1., xUlpDbl / TAYLOR_NTERM));
                for (int i = 1; i <= TAYLOR_NTERM; i++) {
                    ifac = ifac.multiply(new BigInteger("" + i));
                    xpowi = xpowi.multiply(x);
                    final BigDecimal c = xpowi.divide(new BigDecimal(ifac), mcTay);
                    resul = resul.add(c);
                    if (Math.abs(xpowi.doubleValue()) < i && Math.abs(c.doubleValue()) < 0.5 * xUlpDbl) {
                        break;
                    }
                }
                /* exp(x+deltax) = exp(x)(1+deltax) if deltax is <<1. So the relative error
                 * in the result equals the absolute error in the argument.
                 */
                MathContext mc = new MathContext(err2prec(xUlpDbl / 2.));
                return resul.round(mc);
            } else {
                /* Compute exp(x) = (exp(0.1*x))^10. Division by 10 does not lead
                 * to loss of accuracy.
                 */
                int exSc = (int) (1.0 - Math.log10(TAYLOR_NTERM * (TAYLOR_NTERM - 1.0) * (TAYLOR_NTERM - 2.0) * xUlpDbl
                        / Math.pow(xDbl, TAYLOR_NTERM)) / (TAYLOR_NTERM - 1.0));
                BigDecimal xby10 = x.scaleByPowerOfTen(-exSc);
                BigDecimal expxby10 = exp(xby10);
                /* Final powering by 10 means that the relative error of the result
                 * is 10 times the relative error of the base (First order binomial expansion).
                 * This looses one digit.
                 */
                MathContext mc = new MathContext(expxby10.precision() - exSc);
                /* Rescaling the powers of 10 is done in chunks of a maximum of 8 to avoid an invalid operation
                 17
                 * response by the BigDecimal.pow library or integer overflow.
                 */
                while (exSc > 0) {
                    int exsub = Math.min(8, exSc);
                    exSc -= exsub;
                    MathContext mctmp = new MathContext(expxby10.precision() - exsub + 2);
                    int pex = 1;
                    while (exsub-- > 0) {
                        pex *= 10;
                    }
                    expxby10 = expxby10.pow(pex, mctmp);
                }
                return expxby10.round(mc);
            }
        }
    } /* BigDecimalMath.exp */


    /**
     * The natural logarithm.
     *
     * @param x the argument.
     * @return ln(x). The precision of the result is implicitly defined by the
     * precision in the argument.
     */
    public static BigDecimal log(BigDecimal x) {
        /* the value is undefined if x is negative.
         */
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            throw new ArithmeticException("Cannot take log of negative " + x.toString());
        } else if (x.compareTo(BigDecimal.ONE) == 0) {
            /* log 1. = 0. */
            return scalePrec(BigDecimal.ZERO, x.precision() - 1);
        } else if (Math.abs(x.doubleValue() - 1.0) <= 0.3) {
            /* The standard Taylor series around x=1, z=0, z=x-1. Abramowitz-Stegun 4.124.
             * The absolute error is err(z)/(1+z) = err(x)/x.
             */
            BigDecimal z = scalePrec(x.subtract(BigDecimal.ONE), 2);
            BigDecimal zpown = z;
            double eps = 0.5 * x.ulp().doubleValue() / Math.abs(x.doubleValue());
            BigDecimal resul = z;
            for (int k = 2;; k++) {
                zpown = multiplyRound(zpown, z);
                BigDecimal c = divideRound(zpown, k);
                if (k % 2 == 0) {
                    resul = resul.subtract(c);
                } else {
                    resul = resul.add(c);
                }
                if (Math.abs(c.doubleValue()) < eps) {
                    break;
                }
            }
            MathContext mc = new MathContext(err2prec(resul.doubleValue(), eps));
            return resul.round(mc);
        } else {
            final double xDbl = x.doubleValue();
            final double xUlpDbl = x.ulp().doubleValue();
            /* Map log(x) = log root[r](x)^r = r*log( root[r](x)) with the aim
             * to move roor[r](x) near to 1.2 (that is, below the 0.3 appearing above), where log(1.2) is roughly 0.2.
             */
            int r = (int) (Math.log(xDbl) / 0.2);
            /* Since the actual requirement is a function of the value 0.3 appearing above,
             * we avoid the hypothetical case of endless recurrence by ensuring that r >= 2.
             */
            r = Math.max(2, r);
            /* Compute r-th root with 2 additional digits of precision
             */
            BigDecimal xhighpr = scalePrec(x, 2);
            BigDecimal resul = root(r, xhighpr);
            resul = log(resul).multiply(new BigDecimal(r));
            /* error propagation: log(x+errx) = log(x)+errx/x, so the absolute error
             * in the result equals the relative error in the input, xUlpDbl/xDbl .
             */
            MathContext mc = new MathContext(err2prec(resul.doubleValue(), xUlpDbl / xDbl));
            return resul.round(mc);
        }
    } /* BigDecimalMath.log */


    /**
     * Append decimal zeros to the value. This returns a value which appears to
     * have a higher precision than the input.
     *
     * @param x The input value
     * @param d The (positive) value of zeros to be added as least significant
     * digits.
     * @return The same value as the input but with increased (pseudo)
     * precision.
     */
    private static BigDecimal scalePrec(final BigDecimal x, int d) {
        return x.setScale(d + x.scale());

    }

    /**
     * Convert an absolute error to a precision.
     *
     * @param x The value of the variable
     * @param xerr The absolute error in the variable
     * @return The number of valid digits in x. The value is rounded down, and
     * on the pessimistic side for that reason.
     */
    private static int err2prec(BigDecimal x, BigDecimal xerr) {
        return err2prec(xerr.divide(x, MathContext.DECIMAL64).doubleValue());

    }

    /**
     * Convert a relative error to a precision.
     *
     * @param xerr The relative error in the variable. The value returned
     * depends only on the absolute value, not on the sign.
     * @return The number of valid digits in x. The value is rounded down, and
     * on the pessimistic side for that reason.
     */
    private static int err2prec(double xerr) {
        /* Example: an error of xerr=+-0.5 a precision of 1 (digit), an error of
         * +-0.05 a precision of 2 (digits)
         */
        return 1 + (int) (Math.log10(Math.abs(0.5 / xerr)));

    }

    /**
     * Convert an absolute error to a precision.
     *
     * @param x The value of the variable The value returned depends only on the
     * absolute value, not on the sign.
     * @param xerr The absolute error in the variable The value returned depends
     * only on the absolute value, not on the sign.
     * @return The number of valid digits in x. Derived from the representation
     * x+- xerr, as if the error was represented 38 in a "half width" (half of
     * the error bar) form. The value is rounded down, and on the pessimistic
     * side for that reason.
     */
    private static int err2prec(double x, double xerr) {
        /* Example: an error of xerr=+-0.5 at x=100 represents 100+-0.5 with
         * a precision = 3 (digits).
         */
        return 1 + (int) (Math.log10(Math.abs(0.5 * x / xerr)));

    }

    /**
     * Multiply and round.
     *
     * @param x The left factor.
     * @param y The right factor.
     * @return The product x*y.
     */
    private static BigDecimal multiplyRound(final BigDecimal x, final BigDecimal y) {
        BigDecimal resul = x.multiply(y);


        /* The estimation of the relative error in the result is the sum of the relative
         * errors |err(y)/y|+|err(x)/x|
         */
        MathContext mc = new MathContext(Math.min(x.precision(), y.precision()));

        return resul.round(mc);

    } /* multiplyRound */


    /**
     * Divide and round.
     *
     * @param x The numerator
     * @param n The denominator
     * @return the divided x/n
     */
    private static BigDecimal divideRound(final BigDecimal x, final int n) {
        /* The estimation of the relative error in the result is |err(x)/x|
         */
        MathContext mc = new MathContext(x.precision());

        return x.divide(new BigDecimal(n), mc);

    }

} /* BigDecimalMath */
