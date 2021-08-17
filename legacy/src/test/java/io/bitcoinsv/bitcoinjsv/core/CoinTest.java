/*
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.bitcoinsv.bitcoinjsv.core;

import static io.bitcoinsv.bitcoinjsv.params.NetworkParameters.MAX_MONEY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

public class CoinTest {

    @Test
    public void testParseCoin() {
        // String version
        Assert.assertEquals(Coin.CENT, Coin.parseCoin("0.01"));
        Assert.assertEquals(Coin.CENT, Coin.parseCoin("1E-2"));
        Assert.assertEquals(Coin.COIN.add(Coin.CENT), Coin.parseCoin("1.01"));
        Assert.assertEquals(Coin.COIN.negate(), Coin.parseCoin("-1"));
        try {
            Coin.parseCoin("2E-20");
            org.junit.Assert.fail("should not have accepted fractional satoshis");
        } catch (IllegalArgumentException expected) {
        } catch (Exception e) {
            org.junit.Assert.fail("should throw IllegalArgumentException");
        }
    }

    @Test
    public void testValueOf() {
        // int version
        Assert.assertEquals(Coin.CENT, Coin.valueOf(0, 1));
        Assert.assertEquals(Coin.SATOSHI, Coin.valueOf(1));
        Assert.assertEquals(Coin.NEGATIVE_SATOSHI, Coin.valueOf(-1));
        assertEquals(MAX_MONEY, Coin.valueOf(MAX_MONEY.value));
        assertEquals(MAX_MONEY.negate(), Coin.valueOf(MAX_MONEY.value * -1));
        Coin.valueOf(MAX_MONEY.value + 1);
        Coin.valueOf((MAX_MONEY.value * -1) - 1);
        Coin.valueOf(Long.MAX_VALUE);
        Coin.valueOf(Long.MIN_VALUE);

        try {
            Coin.valueOf(1, -1);
            fail();
        } catch (IllegalArgumentException e) {}
        try {
            Coin.valueOf(-1, 0);
            fail();
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void testOperators() {
        assertTrue(Coin.SATOSHI.isPositive());
        assertFalse(Coin.SATOSHI.isNegative());
        assertFalse(Coin.SATOSHI.isZero());
        assertFalse(Coin.NEGATIVE_SATOSHI.isPositive());
        assertTrue(Coin.NEGATIVE_SATOSHI.isNegative());
        assertFalse(Coin.NEGATIVE_SATOSHI.isZero());
        assertFalse(Coin.ZERO.isPositive());
        assertFalse(Coin.ZERO.isNegative());
        assertTrue(Coin.ZERO.isZero());

        assertTrue(Coin.valueOf(2).isGreaterThan(Coin.valueOf(1)));
        assertFalse(Coin.valueOf(2).isGreaterThan(Coin.valueOf(2)));
        assertFalse(Coin.valueOf(1).isGreaterThan(Coin.valueOf(2)));
        assertTrue(Coin.valueOf(1).isLessThan(Coin.valueOf(2)));
        assertFalse(Coin.valueOf(2).isLessThan(Coin.valueOf(2)));
        assertFalse(Coin.valueOf(2).isLessThan(Coin.valueOf(1)));
    }

    @Test(expected = ArithmeticException.class)
    public void testMultiplicationOverflow() {
        Coin.valueOf(Long.MAX_VALUE).multiply(2);
    }

    @Test(expected = ArithmeticException.class)
    public void testMultiplicationUnderflow() {
        Coin.valueOf(Long.MIN_VALUE).multiply(2);
    }

    @Test(expected = ArithmeticException.class)
    public void testAdditionOverflow() {
        Coin.valueOf(Long.MAX_VALUE).add(Coin.SATOSHI);
    }

    @Test(expected = ArithmeticException.class)
    public void testSubstractionUnderflow() {
        Coin.valueOf(Long.MIN_VALUE).subtract(Coin.SATOSHI);
    }

    @Test
    public void testToFriendlyString() {
        Assert.assertEquals("1.00 BCH", Coin.COIN.toFriendlyString());
        Assert.assertEquals("1.23 BCH", Coin.valueOf(1, 23).toFriendlyString());
        Assert.assertEquals("0.001 BCH", Coin.COIN.divide(1000).toFriendlyString());
        Assert.assertEquals("-1.23 BCH", Coin.valueOf(1, 23).negate().toFriendlyString());
    }

    /**
     * Test the bitcoinValueToPlainString amount formatter
     */
    @Test
    public void testToPlainString() {
        assertEquals("0.0015", Coin.valueOf(150000).toPlainString());
        Assert.assertEquals("1.23", Coin.parseCoin("1.23").toPlainString());

        Assert.assertEquals("0.1", Coin.parseCoin("0.1").toPlainString());
        Assert.assertEquals("1.1", Coin.parseCoin("1.1").toPlainString());
        Assert.assertEquals("21.12", Coin.parseCoin("21.12").toPlainString());
        Assert.assertEquals("321.123", Coin.parseCoin("321.123").toPlainString());
        Assert.assertEquals("4321.1234", Coin.parseCoin("4321.1234").toPlainString());
        Assert.assertEquals("54321.12345", Coin.parseCoin("54321.12345").toPlainString());
        Assert.assertEquals("654321.123456", Coin.parseCoin("654321.123456").toPlainString());
        Assert.assertEquals("7654321.1234567", Coin.parseCoin("7654321.1234567").toPlainString());
        Assert.assertEquals("87654321.12345678", Coin.parseCoin("87654321.12345678").toPlainString());

        // check there are no trailing zeros
        Assert.assertEquals("1", Coin.parseCoin("1.0").toPlainString());
        Assert.assertEquals("2", Coin.parseCoin("2.00").toPlainString());
        Assert.assertEquals("3", Coin.parseCoin("3.000").toPlainString());
        Assert.assertEquals("4", Coin.parseCoin("4.0000").toPlainString());
        Assert.assertEquals("5", Coin.parseCoin("5.00000").toPlainString());
        Assert.assertEquals("6", Coin.parseCoin("6.000000").toPlainString());
        Assert.assertEquals("7", Coin.parseCoin("7.0000000").toPlainString());
        Assert.assertEquals("8", Coin.parseCoin("8.00000000").toPlainString());
    }
}
