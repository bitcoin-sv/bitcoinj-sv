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

package io.bitcoinj.core;

import io.bitcoinj.exception.WrongNetworkException;
import io.bitcoinj.params.MainNetParams;
import io.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class DumpedPrivateKeyTest {

    private static final MainNetParams MAINNET = MainNetParams.get();
    private static final TestNet3Params TESTNET = TestNet3Params.get();

    @Test
    public void checkNetwork() {
        DumpedPrivateKeyLite.fromBase58(MAINNET, "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk");
    }

    @Test
    public void checkNetworkWrong() {
        Assertions.assertThrows(WrongNetworkException.class, () ->
                DumpedPrivateKeyLite.fromBase58(TESTNET, "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk")
        );
    }

    @Test
    public void testJavaSerialization() throws Exception {
        DumpedPrivateKeyLite key = new DumpedPrivateKeyLite(MAINNET, new ECKeyLite().getPrivKeyBytes(), true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(key);
        DumpedPrivateKeyLite keyCopy = (DumpedPrivateKeyLite) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(key, keyCopy);
    }

    @Test
    public void cloning() throws Exception {
        DumpedPrivateKeyLite a = new DumpedPrivateKeyLite(MAINNET, new ECKeyLite().getPrivKeyBytes(), true);
        // TODO: Consider overriding clone() in DumpedPrivateKey to narrow the type
        DumpedPrivateKeyLite b = (DumpedPrivateKeyLite) a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() {
        String base58 = "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk";
        assertEquals(base58, DumpedPrivateKeyLite.fromBase58(null, base58).toBase58());
    }
}
