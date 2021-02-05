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

package io.bitcoinj.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


import io.bitcoinj.core.Coin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExchangeRateTest {

    @Test
    public void normalRate() {
        ExchangeRate rate = new ExchangeRate(Fiat.parseFiat("EUR", "500"));
        assertEquals("0.5", rate.coinToFiat(Coin.MILLICOIN).toPlainString());
        assertEquals("0.002", rate.fiatToCoin(Fiat.parseFiat("EUR", "1")).toPlainString());
    }

    @Test
    public void bigRate(){
        ExchangeRate rate = new ExchangeRate(Coin.parseCoin("0.0001"), Fiat.parseFiat("BYR", "5320387.3"));
        assertEquals("53203873000", rate.coinToFiat(Coin.COIN).toPlainString());
        assertEquals("0", rate.fiatToCoin(Fiat.parseFiat("BYR", "1")).toPlainString()); // Tiny value!
    }

    @Test
    public void smallRate(){
        ExchangeRate rate = new ExchangeRate(Coin.parseCoin("1000"), Fiat.parseFiat("XXX", "0.0001"));
        assertEquals("0", rate.coinToFiat(Coin.COIN).toPlainString()); // Tiny value!
        assertEquals("10000000", rate.fiatToCoin(Fiat.parseFiat("XXX", "1")).toPlainString());
    }

    @Test
    public void currencyCodeMismatch() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            ExchangeRate rate = new ExchangeRate(Fiat.parseFiat("EUR", "500"));
            rate.fiatToCoin(Fiat.parseFiat("USD", "1"));
        });
    }

    @Test
    public void constructMissingCurrencyCode() {
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(Fiat.valueOf(null, 1)));
    }

    @Test
    public void constructNegativeCoin() {
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(Coin.valueOf(-1), Fiat.valueOf("EUR", 1)));
    }

    @Test
    public void constructFiatCoin() {
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(Fiat.valueOf("EUR", -1)));
    }

    @Test
    public void testJavaSerialization() throws Exception {
        ExchangeRate rate = new ExchangeRate(Fiat.parseFiat("EUR", "500"));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(rate);
        ExchangeRate rateCopy = (ExchangeRate) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(rate, rateCopy);
    }
}
