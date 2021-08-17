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

package io.bitcoinsv.bitcoinjsv.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class FiatTest {

    @Test
    public void testParseAndValueOf() {
        Assert.assertEquals(Fiat.valueOf("EUR", 10000), Fiat.parseFiat("EUR", "1"));
        Assert.assertEquals(Fiat.valueOf("EUR", 100), Fiat.parseFiat("EUR", "0.01"));
        Assert.assertEquals(Fiat.valueOf("EUR", 1), Fiat.parseFiat("EUR", "0.0001"));
        Assert.assertEquals(Fiat.valueOf("EUR", -10000), Fiat.parseFiat("EUR", "-1"));
    }

    @Test
    public void testToFriendlyString() {
        Assert.assertEquals("1.00 EUR", Fiat.parseFiat("EUR", "1").toFriendlyString());
        Assert.assertEquals("1.23 EUR", Fiat.parseFiat("EUR", "1.23").toFriendlyString());
        Assert.assertEquals("0.0010 EUR", Fiat.parseFiat("EUR", "0.001").toFriendlyString());
        Assert.assertEquals("-1.23 EUR", Fiat.parseFiat("EUR", "-1.23").toFriendlyString());
    }

    @Test
    public void testToPlainString() {
        assertEquals("0.0015", Fiat.valueOf("EUR", 15).toPlainString());
        Assert.assertEquals("1.23", Fiat.parseFiat("EUR", "1.23").toPlainString());

        Assert.assertEquals("0.1", Fiat.parseFiat("EUR", "0.1").toPlainString());
        Assert.assertEquals("1.1", Fiat.parseFiat("EUR", "1.1").toPlainString());
        Assert.assertEquals("21.12", Fiat.parseFiat("EUR", "21.12").toPlainString());
        Assert.assertEquals("321.123", Fiat.parseFiat("EUR", "321.123").toPlainString());
        Assert.assertEquals("4321.1234", Fiat.parseFiat("EUR", "4321.1234").toPlainString());

        // check there are no trailing zeros
        Assert.assertEquals("1", Fiat.parseFiat("EUR", "1.0").toPlainString());
        Assert.assertEquals("2", Fiat.parseFiat("EUR", "2.00").toPlainString());
        Assert.assertEquals("3", Fiat.parseFiat("EUR", "3.000").toPlainString());
        Assert.assertEquals("4", Fiat.parseFiat("EUR", "4.0000").toPlainString());
    }
}
