/*
 * Copyright 2011 Google Inc.
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

import io.bitcoinj.exception.AddressFormatException;
import io.bitcoinj.exception.WrongNetworkException;
import io.bitcoinj.params.*;
import io.bitcoinj.script.Script;
import io.bitcoinj.script.ScriptBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static io.bitcoinj.core.Utils.HEX;
import static org.junit.jupiter.api.Assertions.*;

public class AddressTest {
    static final NetworkParameters testParams = TestNet3Params.get();
    static final NetworkParameters mainParams = MainNetParams.get();

    @Test
    public void testJavaSerialization() throws Exception {
        AddressLite testAddress = AddressLite.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testAddress);
        VersionedChecksummedBytes testAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(testAddress, testAddressCopy);

        AddressLite mainAddress = AddressLite.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainAddress);
        VersionedChecksummedBytes mainAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(mainAddress, mainAddressCopy);
    }

    @Test
    public void stringification() {
        // Test a testnet address.
        AddressLite a = new AddressLite(testParams, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        assertEquals("n4eA2nbYqErp7H6jebchxAN59DmNpksexv", a.toString());
        assertFalse(a.isP2SHAddress());

        AddressLite b = new AddressLite(mainParams, HEX.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));
        assertEquals("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL", b.toString());
        assertFalse(b.isP2SHAddress());
    }

    @Test
    public void decoding() {
        AddressLite a = AddressLite.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", Utils.HEX.encode(a.getHash160()));

        AddressLite b = AddressLite.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals("4a22c3c4cbb31e4d03b15550636762bda0baf85a", Utils.HEX.encode(b.getHash160()));
    }

    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            AddressLite.fromBase58(testParams, "this is not a valid address!");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            AddressLite.fromBase58(testParams, "");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            AddressLite.fromBase58(testParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
            fail();
        } catch (WrongNetworkException e) {
            // Success.
            assertEquals(e.verCode, MainNetParams.get().getAddressHeader());
            assertTrue(Arrays.equals(e.acceptableVersions, TestNet3Params.get().getAcceptableAddressCodes()));
        } catch (AddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() {
        NetworkParameters params = AddressLite.getParametersFromAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals(MainNetParams.get().getId(), params.getId());
        params = AddressLite.getParametersFromAddress("n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals(TestNet3Params.get().getId(), params.getId());
    }

    @Test
    public void getAltNetwork() {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super(Net.MAINNET);
                id = "alt.network";
                addressHeader = 48;
                p2shHeader = 5;
                acceptableAddressCodes = new int[]{addressHeader, p2shHeader};
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse address
        NetworkParameters params = AddressLite.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        assertEquals(altNetwork.getId(), params.getId());
        // Check if main network works as before
        params = AddressLite.getParametersFromAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals(MainNetParams.get().getId(), params.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            AddressLite.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (AddressFormatException e) {
        }
    }

    @Test
    public void p2shAddress() {
        // Test that we can construct P2SH addresses
        AddressLite mainNetP2SHAddress = AddressLite.fromBase58(MainNetParams.get(), "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(mainNetP2SHAddress.version, MainNetParams.get().getP2SHHeader());
        assertTrue(mainNetP2SHAddress.isP2SHAddress());
        AddressLite testNetP2SHAddress = AddressLite.fromBase58(TestNet3Params.get(), "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(testNetP2SHAddress.version, TestNet3Params.get().getP2SHHeader());
        assertTrue(testNetP2SHAddress.isP2SHAddress());

        // Test that we can determine what network a P2SH address belongs to
        NetworkParameters mainNetParams = AddressLite.getParametersFromAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(MainNetParams.get().getId(), mainNetParams.getId());
        NetworkParameters testNetParams = AddressLite.getParametersFromAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(TestNet3Params.get().getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("2ac4b0b501117cc8119c5797b519538d4942e90e");
        AddressLite a = AddressLite.fromP2SHHash(mainParams, hex);
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", a.toString());
        AddressLite b = AddressLite.fromP2SHHash(testParams, HEX.decode("18a0e827269b5211eb51a4af1b2fa69333efa722"));
        assertEquals("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe", b.toString());
        AddressLite c = AddressLite.fromP2SHScript(mainParams, ScriptBuilder.createP2SHOutputScript(hex));
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", c.toString());
    }

    @Test
    public void p2shAddressCreationFromKeys() {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        ECKeyLite key1 = DumpedPrivateKeyLite.fromBase58(mainParams, "5JaTXbAUmfPYZFRwrYaALK48fN6sFJp4rHqq2QSXs8ucfpE4yQU").getKey();
        key1 = ECKeyLite.fromPrivate(key1.getPrivKeyBytes());
        ECKeyLite key2 = DumpedPrivateKeyLite.fromBase58(mainParams, "5Jb7fCeh1Wtm4yBBg3q3XbT6B525i17kVhy3vMC9AqfR6FH2qGk").getKey();
        key2 = ECKeyLite.fromPrivate(key2.getPrivKeyBytes());
        ECKeyLite key3 = DumpedPrivateKeyLite.fromBase58(mainParams, "5JFjmGo5Fww9p8gvx48qBYDJNAzR9pmH5S389axMtDyPT8ddqmw").getKey();
        key3 = ECKeyLite.fromPrivate(key3.getPrivKeyBytes());

        List<ECKeyLite> keys = Arrays.asList(key1, key2, key3);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(2, keys);
        AddressLite address = AddressLite.fromP2SHScript(mainParams, p2shScript);
        assertEquals("3N25saC4dT24RphDAwLtD8LUN4E2gZPJke", address.toString());
    }

    @Test
    public void cloning() throws Exception {
        AddressLite a = new AddressLite(testParams, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        AddressLite b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() {
        String base58 = "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL";
        assertEquals(base58, AddressLite.fromBase58(null, base58).toBase58());
    }

    @Test
    public void comparisonCloneEqualTo() throws Exception {
        AddressLite a = AddressLite.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        AddressLite b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonEqualTo() throws Exception {
        AddressLite a = AddressLite.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        AddressLite b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonLessThan() {
        AddressLite a = AddressLite.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        AddressLite b = AddressLite.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");

        int result = a.compareTo(b);
        assertTrue(result < 0);
    }

    @Test
    public void comparisonGreaterThan() {
        AddressLite a = AddressLite.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");
        AddressLite b = AddressLite.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");

        int result = a.compareTo(b);
        assertTrue(result > 0);
    }

    @Test
    public void comparisonBytesVsString() {
        // TODO: To properly test this we need a much larger data set
        AddressLite a = AddressLite.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        AddressLite b = AddressLite.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");

        int resultBytes = a.compareTo(b);
        int resultsString = a.toString().compareTo(b.toString());
        assertTrue(resultBytes < 0);
        assertTrue(resultsString < 0);
    }
}
