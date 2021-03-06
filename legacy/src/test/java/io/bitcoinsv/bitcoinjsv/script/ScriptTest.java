/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2017 Thomas König
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

package io.bitcoinsv.bitcoinjsv.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bitcoinsv.bitcoinjsv.ecc.ECDSASignature;
import io.bitcoinsv.bitcoinjsv.ecc.ECKeyBytes;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.msg.Serializer;
import io.bitcoinsv.bitcoinjsv.msg.Translate;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Transaction;
import io.bitcoinsv.bitcoinjsv.ecc.TransactionSignature;
import io.bitcoinsv.bitcoinjsv.msg.protocol.TransactionInput;
import io.bitcoinsv.bitcoinjsv.msg.protocol.TransactionOutPoint;
import io.bitcoinsv.bitcoinjsv.msg.protocol.TransactionOutput;
import io.bitcoinsv.bitcoinjsv.params.MainNetParams;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;
import io.bitcoinsv.bitcoinjsv.params.TestNet3Params;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.bitcoinsv.bitcoinjsv.script.interpreter.Interpreter;
import io.bitcoinsv.bitcoinjsv.script.interpreter.ScriptExecutionException;
import io.bitcoinsv.bitcoinjsv.script.interpreter.ScriptStack;
import io.bitcoinsv.bitcoinjsv.core.*;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.*;

import static io.bitcoinsv.bitcoinjsv.script.ScriptOpCodes.OP_0;
import static io.bitcoinsv.bitcoinjsv.script.ScriptOpCodes.OP_INVALIDOPCODE;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import org.junit.Before;

public class ScriptTest {
    // From tx 05e04c26c12fe408a3c1b71aa7996403f6acad1045252b1c62e055496f4d2cb1 on the testnet.

    static final String sigProg = "47304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701410414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c";

    static final String pubkeyProg = "76a91433e81a941e64cda12c6a299ed322ddbdd03f8d0e88ac";

    private static final NetworkParameters PARAMS = TestNet3Params.get();
    private static final Net NET = Net.TESTNET3;

    private static final Logger log = LoggerFactory.getLogger(ScriptTest.class);

    @Before
    public void setUp() throws Exception {
        Context context = new Context(PARAMS);
        ScriptStack.VERIFY_STACK_MEMEORY_USAGE = true;
    }

    @Test
    public void testScriptSig() throws Exception {
        byte[] sigProgBytes = Utils.HEX.decode(sigProg);
        Script script = new Script(sigProgBytes);
        // Test we can extract the from address.
        byte[] hash160 = Utils.sha256hash160(script.getPubKey());
        Address a = new Address(PARAMS, hash160);
        assertEquals("mkFQohBpy2HDXrCwyMrYL5RtfrmeiuuPY2", a.toString());
    }

    @Test
    public void testScriptPubKey() throws Exception {
        // Check we can extract the to address
        byte[] pubkeyBytes = Utils.HEX.decode(pubkeyProg);
        Script pubkey = new Script(pubkeyBytes);
        assertEquals("DUP HASH160 PUSHDATA(20)[33e81a941e64cda12c6a299ed322ddbdd03f8d0e] EQUALVERIFY CHECKSIG", pubkey.toString());
        Address toAddr = new Address(PARAMS, pubkey.getPubKeyHash());
        assertEquals("mkFQohBpy2HDXrCwyMrYL5RtfrmeiuuPY2", toAddr.toString());
    }

    @Test
    @SuppressWarnings("TryFailThrowable")       // todo: fix? https://errorprone.info/bugpattern/TryFailThrowable
    public void testMultiSig() throws Exception {
        List<ECKey> keys = Lists.newArrayList(new ECKey(), new ECKey(), new ECKey());
        assertTrue(ScriptBuilder.createMultiSigOutputScript(2, keys).isSentToMultiSig());
        Script script = ScriptBuilder.createMultiSigOutputScript(3, keys);
        assertTrue(script.isSentToMultiSig());
        List<ECKeyBytes> pubkeys = new ArrayList<>(3);
        for (ECKey key : keys) {
            pubkeys.add(new BasicECKeyBytes(ECKey.fromPublicOnly(key.getPubKeyPoint()).getPubKey()));
        }
        assertEquals(ScriptUtils.getPubKeys(script), pubkeys);
        assertFalse(ScriptBuilder.createOutputScript(new ECKey()).isSentToMultiSig());
        try {
            // Fail if we ask for more signatures than keys.
            ScriptUtils.createMultiSigOutputScript(4, keys);
            fail();
        } catch (Throwable e) {
            // Expected.
        }
        try {
            // Must have at least one signature required.
            ScriptUtils.createMultiSigOutputScript(0, keys);
        } catch (Throwable e) {
            // Expected.
        }
        // Actual execution is tested by the data driven tests.
    }

    @Test
    public void testP2SHOutputScript() throws Exception {
        Address p2shAddress = Address.fromBase58(MainNetParams.get(), "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertTrue(ScriptBuilder.createOutputScript(p2shAddress).isPayToScriptHash());
    }

    @Test
    public void testIp() throws Exception {
        byte[] bytes = Utils.HEX.decode("41043e96222332ea7848323c08116dddafbfa917b8e37f0bdf63841628267148588a09a43540942d58d49717ad3fabfe14978cf4f0a8b84d2435dad16e9aa4d7f935ac");
        Script s = new Script(bytes);
        assertTrue(s.isSentToRawPubKey());
    }

    @Test
    public void testCreateMultiSigInputScript() {
        // Setup transaction and signatures
        ECKey key1 = DumpedPrivateKey.fromBase58(PARAMS, "cVLwRLTvz3BxDAWkvS3yzT9pUcTCup7kQnfT2smRjvmmm1wAP6QT").getKey();
        ECKey key2 = DumpedPrivateKey.fromBase58(PARAMS, "cTine92s8GLpVqvebi8rYce3FrUYq78ZGQffBYCS1HmDPJdSTxUo").getKey();
        ECKey key3 = DumpedPrivateKey.fromBase58(PARAMS, "cVHwXSPRZmL9adctwBwmn4oTZdZMbaCsR5XF6VznqMgcvt1FDDxg").getKey();
        Script multisigScript = ScriptBuilder.createMultiSigOutputScript(2, Arrays.asList(key1, key2, key3));
        byte[] bytes = Utils.HEX.decode("01000000013df681ff83b43b6585fa32dd0e12b0b502e6481e04ee52ff0fdaf55a16a4ef61000000006b483045022100a84acca7906c13c5895a1314c165d33621cdcf8696145080895cbf301119b7cf0220730ff511106aa0e0a8570ff00ee57d7a6f24e30f592a10cae1deffac9e13b990012102b8d567bcd6328fd48a429f9cf4b315b859a58fd28c5088ef3cb1d98125fc4e8dffffffff02364f1c00000000001976a91439a02793b418de8ec748dd75382656453dc99bcb88ac40420f000000000017a9145780b80be32e117f675d6e0ada13ba799bf248e98700000000");
        Transaction transaction = Serializer.defaultFor(NET).makeTransaction(bytes);
        TransactionOutput output = transaction.getOutput(1);
        Transaction spendTx = new Transaction(NET);
        Address address = Address.fromBase58(PARAMS, "n3CFiCmBXVt5d3HXKQ15EFZyhPz4yj5F3H");
        Script outputScript = ScriptBuilder.createOutputScript(address);
        spendTx.addOutput(output.getValue(), outputScript);
        spendTx.addInput(output);
        Sha256Hash sighash = Transaction.hashForLegacySignature(spendTx, 0, multisigScript, SigHash.Flags.ALL, false);
        ECDSASignature party1Signature = key1.sign(sighash);
        ECDSASignature party2Signature = key2.sign(sighash);
        TransactionSignature party1TransactionSignature = new TransactionSignature(party1Signature, SigHash.Flags.ALL, false);
        TransactionSignature party2TransactionSignature = new TransactionSignature(party2Signature, SigHash.Flags.ALL, false);

        // Create p2sh multisig input script
        Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(ImmutableList.of(party1TransactionSignature, party2TransactionSignature), multisigScript);

        // Assert that the input script contains 4 chunks
        assertTrue(inputScript.getChunks().size() == 4);

        // Assert that the input script created contains the original multisig
        // script as the last chunk
        ScriptChunk scriptChunk = inputScript.getChunks().get(inputScript.getChunks().size() - 1);
        assertArrayEquals(scriptChunk.data(), multisigScript.getProgram());

        // Create regular multisig input script
        inputScript = ScriptBuilder.createMultiSigInputScript(ImmutableList.of(party1TransactionSignature, party2TransactionSignature));

        // Assert that the input script only contains 3 chunks
        assertTrue(inputScript.getChunks().size() == 3);

        // Assert that the input script created does not end with the original
        // multisig script
        scriptChunk = inputScript.getChunks().get(inputScript.getChunks().size() - 1);
        Assert.assertThat(scriptChunk.data, IsNot.not(equalTo(multisigScript.getProgram())));
    }

    @Test
    public void createAndUpdateEmptyInputScript() throws Exception {
        TransactionSignature dummySig = TransactionSignature.dummy();
        ECKey key = new ECKey();

        // pay-to-pubkey
        Script inputScript = ScriptBuilder.createInputScript(dummySig);
        assertThat(inputScript.getChunks().get(0).data(), equalTo(dummySig.encodeToBitcoin()));
        inputScript = ScriptBuilder.createInputScript(null);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));

        // pay-to-address
        inputScript = ScriptBuilder.createInputScript(dummySig, key);
        assertThat(inputScript.getChunks().get(0).data(), equalTo(dummySig.encodeToBitcoin()));
        inputScript = ScriptBuilder.createInputScript(null, key);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data(), equalTo(key.getPubKey()));

        // pay-to-script-hash
        ECKey key2 = new ECKey();
        Script multisigScript = ScriptBuilder.createMultiSigOutputScript(2, Arrays.asList(key, key2));
        inputScript = ScriptBuilder.createP2SHMultiSigInputScript(Arrays.asList(dummySig, dummySig), multisigScript);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data(), equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(2).data(), equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(3).data(), equalTo(multisigScript.getProgram()));

        inputScript = ScriptBuilder.createP2SHMultiSigInputScript(null, multisigScript);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(2).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(3).data(), equalTo(multisigScript.getProgram()));

        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, dummySig.encodeToBitcoin(), 0, 1, 1);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data(), equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(2).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(3).data(), equalTo(multisigScript.getProgram()));

        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, dummySig.encodeToBitcoin(), 1, 1, 1);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data(), equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(2).data(), equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(3).data(), equalTo(multisigScript.getProgram()));

        // updating scriptSig with no missing signatures
        try {
            ScriptBuilder.updateScriptWithSignature(inputScript, dummySig.encodeToBitcoin(), 1, 1, 1);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    @Test
    public void testOp0() {
        // Check that OP_0 doesn't NPE and pushes an empty stack frame.
        Transaction tx = new Transaction(NET);
        tx.addInput(new TransactionInput(NET, tx, new byte[] {}));
        Script script = new ScriptBuilder().smallNum(0).build();

        ScriptStack stack = new ScriptStack();
        Interpreter.executeScript(Translate.toTx(tx), 0, script, stack, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
        assertEquals("OP_0 push length", 0, stack.get(0).length());
    }


    private Script parseScriptString(String string) throws IOException {
        String[] words = string.split("[ \\t\\n]");

        UnsafeByteArrayOutputStream out = new UnsafeByteArrayOutputStream();

        for(String w : words) {
            if (w.equals(""))
                continue;
            if (w.matches("^-?[0-9]*$")) {
                // Number
                long val = Long.parseLong(w);
                if (val >= -1 && val <= 16)
                    out.write(ScriptOpCodes.encodeToOpN((int)val));
                else
                    ScriptChunk.writeBytes(out, Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(val), false)));
            } else if (w.matches("^0x[0-9a-fA-F]*$")) {
                // Raw hex data, inserted NOT pushed onto stack:
                out.write(Utils.HEX.decode(w.substring(2).toLowerCase()));
            } else if (w.length() >= 2 && w.startsWith("'") && w.endsWith("'")) {
                // Single-quoted string, pushed as data. NOTE: this is poor-man's
                // parsing, spaces/tabs/newlines in single-quoted strings won't work.
                ScriptChunk.writeBytes(out, w.substring(1, w.length() - 1).getBytes(Charset.forName("UTF-8")));
            } else if (ScriptOpCodes.getOpCode(w) != OP_INVALIDOPCODE) {
                // opcode, e.g. OP_ADD or OP_1:
                out.write(ScriptOpCodes.getOpCode(w));
            } else if (w.startsWith("OP_") && ScriptOpCodes.getOpCode(w.substring(3)) != OP_INVALIDOPCODE) {
                // opcode, e.g. OP_ADD or OP_1:
                out.write(ScriptOpCodes.getOpCode(w.substring(3)));
            } else {
                throw new RuntimeException("Invalid Data");
            }
        }

        return new Script(out.toByteArray());
    }

    private Set<ScriptVerifyFlag> parseVerifyFlags(String str) {
        Set<ScriptVerifyFlag> flags = EnumSet.noneOf(ScriptVerifyFlag.class);
        if (!"NONE".equals(str)) {
            for (String flag : str.split(",")) {
                try {
                    flags.add(ScriptVerifyFlag.valueOf(flag));
                } catch (IllegalArgumentException x) {
                    log.debug("Cannot handle verify flag {} -- ignored.", flag);
                }
            }
        }
        return flags;
    }

    @Test
    public void dataDrivenValidScripts() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "script_valid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            testValid(test);
        }
    }

    private void testValid(JsonNode test) throws Exception {
        Script scriptSig = parseScriptString(test.get(0).asText());
        Script scriptPubKey = parseScriptString(test.get(1).asText());
        Set<ScriptVerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());
        try {
            ScriptUtils_legacy.correctlySpends(scriptSig, new Transaction(NET), 0, scriptPubKey, verifyFlags);
        } catch (ScriptExecutionException e) {
            System.err.println(test);
            System.err.flush();
            throw e;
        }
    }

    @Test
    public void dataDrivenInvalidScripts() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "script_invalid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            testInvalid(test);
        }
    }

    private void testInvalid(JsonNode test) throws Exception {
        try {
            Script scriptSig = parseScriptString(test.get(0).asText());
            Script scriptPubKey = parseScriptString(test.get(1).asText());
            Set<ScriptVerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());
            ScriptUtils_legacy.correctlySpends(scriptSig, new Transaction(NET), 0, scriptPubKey, verifyFlags);
            System.err.println(test);
            System.err.flush();
            fail();
        } catch (VerificationException e) {
            // Expected.
        }
    }

    private Map<TransactionOutPoint, Script> parseScriptPubKeys(JsonNode inputs) throws IOException {
        Map<TransactionOutPoint, Script> scriptPubKeys = new HashMap<TransactionOutPoint, Script>();
        for (JsonNode input : inputs) {
            String hash = input.get(0).asText();
            int index = input.get(1).asInt();
            String script = input.get(2).asText();
            Sha256Hash sha256Hash = Sha256Hash.wrap(Utils.HEX.decode(hash));
            scriptPubKeys.put(new TransactionOutPoint(NET, index, sha256Hash), parseScriptString(script));
        }
        return scriptPubKeys;
    }

    @Test
    public void dataDrivenValidTransactions() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "tx_valid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            if (test.isArray() && test.size() == 1 && test.get(0).isTextual())
                continue; // This is a comment.
            Transaction transaction = null;
            try {
                Map<TransactionOutPoint, Script> scriptPubKeys = parseScriptPubKeys(test.get(0));
                transaction = Serializer.defaultFor(NET).makeTransaction(Utils.HEX.decode(test.get(1).asText().toLowerCase()));
                transaction.verify();
                Set<ScriptVerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());

                for (int i = 0; i < transaction.getInputs().size(); i++) {
                    TransactionInput input = transaction.getInputs().get(i);
                    if (input.getOutpoint().getIndex() == 0xffffffffL)
                        input.getOutpoint().setIndex(-1);
                    assertTrue(scriptPubKeys.containsKey(input.getOutpoint()));
                    ScriptUtils_legacy.correctlySpends(input.getScriptSig(), transaction, i, scriptPubKeys.get(input.getOutpoint()),
                            verifyFlags);
                }
            } catch (Exception e) {
                System.err.println(test);
                if (transaction != null)
                    System.err.println(transaction);
                throw e;
            }
        }
    }

    @Test
    public void dataDrivenInvalidTransactions() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "tx_invalid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            if (test.isArray() && test.size() == 1 && test.get(0).isTextual())
                continue; // This is a comment.
            Map<TransactionOutPoint, Script> scriptPubKeys = parseScriptPubKeys(test.get(0));
            Transaction transaction = Serializer.defaultFor(NET).makeTransaction(Utils.HEX.decode(test.get(1).asText().toLowerCase()));
            Set<ScriptVerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());

            boolean valid = true;
            try {
                transaction.verify();
            } catch (VerificationException e) {
                valid = false;
            }

            // Bitcoin Core checks this case in CheckTransaction, but we leave it to
            // later where we will see an attempt to double-spend, so we explicitly check here
            HashSet<TransactionOutPoint> set = new HashSet<TransactionOutPoint>();
            for (TransactionInput input : transaction.getInputs()) {
                if (set.contains(input.getOutpoint()))
                    valid = false;
                set.add(input.getOutpoint());
            }

            for (int i = 0; i < transaction.getInputs().size() && valid; i++) {
                TransactionInput input = transaction.getInputs().get(i);
                assertTrue(scriptPubKeys.containsKey(input.getOutpoint()));
                try {
                    ScriptUtils_legacy.correctlySpends(input.getScriptSig(), transaction, i, scriptPubKeys.get(input.getOutpoint()),
                            verifyFlags);
                } catch (VerificationException e) {
                    valid = false;
                }
            }

            if (valid)
                fail();
        }
    }

    @Test
    public void testCLTVPaymentChannelOutput() {
        Script script = ScriptBuilder.createCLTVPaymentChannelOutput(BigInteger.valueOf(20), new ECKey(), new ECKey());
        assertTrue("script is locktime-verify", script.isSentToCLTVPaymentChannel());
    }

    @Test
    public void getToAddress() throws Exception {
        // pay to pubkey
        ECKey toKey = new ECKey();
        Address toAddress = toKey.toAddress(PARAMS);
        assertEquals(toAddress.toBase58(), ScriptUtils.getToAddress(ScriptBuilder.createOutputScript(toKey), PARAMS, true).toBase58());
        // pay to pubkey hash
        assertEquals(toAddress.toBase58(), ScriptUtils.getToAddress(ScriptBuilder.createOutputScript(toAddress), PARAMS, true).toBase58());
        // pay to script hash
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(new byte[20]);
        Address scriptAddress = Address.fromP2SHScript(PARAMS, p2shScript);
        assertEquals(scriptAddress.toBase58(), ScriptUtils.getToAddress(p2shScript, PARAMS, true).toBase58());
    }

    @Test(expected = ScriptExecutionException.class)
    public void getToAddressNoPubKey() throws Exception {
        ScriptUtils.getToAddress(ScriptBuilder.createOutputScript(new ECKey()), PARAMS, false);
    }

    /** Test encoding of zero, which should result in an opcode */
    @Test
    public void numberBuilderZero() {
        final ScriptBuilder builder = new ScriptBuilder();

        // 0 should encode directly to 0
        builder.number(0);
        assertArrayEquals(new byte[] {
                0x00         // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void numberBuilderPositiveOpCode() {
        final ScriptBuilder builder = new ScriptBuilder();

        builder.number(5);
        assertArrayEquals(new byte[] {
                0x55         // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void numberBuilderBigNum() {
        ScriptBuilder builder = new ScriptBuilder();
        // 21066 should take up three bytes including the length byte
        // at the start

        builder.number(0x524a);
        assertArrayEquals(new byte[] {
                0x02,             // Length of the pushed data
                0x4a, 0x52        // Pushed data
        }, builder.build().getProgram());

        // Test the trimming code ignores zeroes in the middle
        builder = new ScriptBuilder();
        builder.number(0x110011);
        assertEquals(4, builder.build().getProgram().length);

        // Check encoding of a value where signed/unsigned encoding differs
        // because the most significant byte is 0x80, and therefore a
        // sign byte has to be added to the end for the signed encoding.
        builder = new ScriptBuilder();
        builder.number(0x8000);
        assertArrayEquals(new byte[] {
                0x03,             // Length of the pushed data
                0x00, (byte) 0x80, 0x00  // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void numberBuilderNegative() {
        // Check encoding of a negative value
        final ScriptBuilder builder = new ScriptBuilder();
        builder.number(-5);
        assertArrayEquals(new byte[] {
                0x01,        // Length of the pushed data
                ((byte) 133) // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void numberBuilder16() {
        ScriptBuilder builder = new ScriptBuilder();
        // Numbers greater than 16 must be encoded with PUSHDATA
        builder.number(15).number(16).number(17);
        builder.number(0, 17).number(1, 16).number(2, 15);
        Script script = builder.build();
        assertEquals("PUSHDATA(1)[11] 16 15 15 16 PUSHDATA(1)[11]", script.toString());
    }

    /** Bitwise ops **/

    static final int MAX_BITWISE_RANDOM_TESTS = 2000;

    @Test
    public void testBitwiseRandomData() throws IOException {
        byte[] a = new byte[MAX_BITWISE_RANDOM_TESTS];
        byte[] b = new byte[MAX_BITWISE_RANDOM_TESTS];
        new Random(0).nextBytes(a); //using the same seed always generates the same byte array
        new Random(1).nextBytes(b);

        for (int x = 0 ; x < MAX_BITWISE_RANDOM_TESTS ; x++) {
            byte aandb = (byte)(a[x] & b[x]);
            byte aorb  = (byte)(a[x] | b[x]);
            byte axorb = (byte)(a[x] ^ b[x]);

            Assert.assertEquals(generateAndExecuteBitwiseScript(a[x], b[x], "AND"), aandb);
            Assert.assertEquals(generateAndExecuteBitwiseScript(b[x], a[x], "AND"), aandb);
            Assert.assertEquals(generateAndExecuteBitwiseScript(a[x], b[x], "OR"), aorb);
            Assert.assertEquals(generateAndExecuteBitwiseScript(b[x], a[x], "OR"), aorb);
            Assert.assertEquals(generateAndExecuteBitwiseScript(a[x], b[x], "XOR"), axorb);
            Assert.assertEquals(generateAndExecuteBitwiseScript(b[x], a[x], "XOR"), axorb);
        }
    }

    @Test
    public void testBitwiseOpcodes() {
        for (int x = 0; x < ScriptTestBitwiseData.a.length ; x++) {
            byte a = (byte) ScriptTestBitwiseData.a[x];
            byte b = (byte) ScriptTestBitwiseData.b[x];
            byte expected_xor = (byte)(a^b);

            Assert.assertEquals(generateAndExecuteBitwiseScript(a, b, "AND"), (byte)ScriptTestBitwiseData.aandb[x]);
            Assert.assertEquals(generateAndExecuteBitwiseScript(b, a, "AND"), (byte)ScriptTestBitwiseData.aandb[x]);
            Assert.assertEquals(generateAndExecuteBitwiseScript(a, b, "OR"), (byte)ScriptTestBitwiseData.aorb[x]);
            Assert.assertEquals(generateAndExecuteBitwiseScript(b, a, "OR"), (byte)ScriptTestBitwiseData.aorb[x]);
            Assert.assertEquals(generateAndExecuteBitwiseScript(a, b, "XOR"), expected_xor);
            Assert.assertEquals(generateAndExecuteBitwiseScript(b, a, "XOR"), expected_xor);
        }
    }

    private byte generateAndExecuteBitwiseScript(byte a, byte b, String opcode) {
        byte[] result = generateAndExecuteBitwiseScript(new byte[]{a}, new byte[]{b}, opcode);
        return result[0];
    }

    private byte[] generateAndExecuteBitwiseScript(byte[] a, byte[] b, String opcode) {
        Script script = new ScriptBuilder().data(a).data(b).op(ScriptOpCodes.getOpCode(opcode)).build();
        ScriptStack stack = new ScriptStack();
        EnumSet<ScriptVerifyFlag> verifyFlags = EnumSet.noneOf(ScriptVerifyFlag.class);
        verifyFlags.add(ScriptVerifyFlag.MONOLITH_OPCODES);
        Interpreter.executeScript(Translate.toTx(new Transaction(NET)), 0, script, stack, Coin.ZERO, verifyFlags);
        Assert.assertEquals("Stack size must be 1", stack.size(), 1);
        return stack.peekLast().bytes();
    }


    /** Number encoding **/

    public void checkMinimallyEncoded(byte[] data, byte[] expected) {
        boolean alreadyEncoded = Utils.checkMinimallyEncodedLE(data, data.length);
        byte[] encoded = Utils.minimallyEncodeLE(data);
        boolean hasEncoded = data.length != encoded.length;
        assertEquals(hasEncoded, !alreadyEncoded);
        assertArrayEquals(encoded, expected);
    }

    @Test
    public void minimizeEncodingTest() {
        checkMinimallyEncoded(new byte[0], new byte[0]);

        try {

            UnsafeByteArrayOutputStream zero = new UnsafeByteArrayOutputStream();
            UnsafeByteArrayOutputStream negZero = new UnsafeByteArrayOutputStream();

            //This limit is enough to get us into the range of PUSHDATA4
            long limit = Short.MAX_VALUE * 2 + Interpreter.MAX_SCRIPT_ELEMENT_SIZE;

            for (int i = 0; i < limit; i++) {
                //speed up tests a bit since we don't need to test every number in range
                if (i > Interpreter.MAX_SCRIPT_ELEMENT_SIZE)
                    i = i + 10;

                zero.write(0x00);
                checkMinimallyEncoded(zero.toByteArray(), new byte[0]);

                negZero.write(0x80);
                checkMinimallyEncoded(negZero.toByteArray(), new byte[0]);

                //reset negZero for next round
                int len = negZero.size();
                negZero.reset();
                negZero.write(new byte[len]);

            }

            // Keep one leading zero when sign bit is used.
            byte[] n = new byte[]{(byte) 0x80, (byte) 0x00};
            byte[] negn = new byte[]{(byte) 0x80, (byte) 0x80};
            UnsafeByteArrayOutputStream nPadded = new UnsafeByteArrayOutputStream();
            nPadded.write(n);
            UnsafeByteArrayOutputStream negnPadded = new UnsafeByteArrayOutputStream();
            negnPadded.write(negn);

            for (int i = 0; i < limit; i++) {
                //speed up tests a bit since we don't need to test every number in range
                if (i > Interpreter.MAX_SCRIPT_ELEMENT_SIZE)
                    i = i + 10;

                checkMinimallyEncoded(nPadded.toByteArray(), n);
                nPadded.write(0x00);

                byte[] negnPaddedBytes = negnPadded.toByteArray();
                checkMinimallyEncoded(negnPaddedBytes, negn);

                //reset to move the 0x80 one to the right
                negnPadded.reset();
                negnPadded.write(negnPaddedBytes, 0, negnPaddedBytes.length - 1);
                negnPadded.write(0x00);
                negnPadded.write(0x80);
            }

            // Merge leading byte when sign bit isn't used
            byte[] k = new byte[]{(byte) 0x7f};
            byte[] negk = new byte[]{(byte) 0xff};
            UnsafeByteArrayOutputStream kPadded = new UnsafeByteArrayOutputStream();
            kPadded.write(k);
            UnsafeByteArrayOutputStream negkPadded = new UnsafeByteArrayOutputStream();
            negkPadded.write(negk);

            for (int i = 0; i < limit; i++) {
                //speed up tests a bit since we don't need to test every number in range
                if (i > Interpreter.MAX_SCRIPT_ELEMENT_SIZE)
                    i = i + 100;

                checkMinimallyEncoded(kPadded.toByteArray(), k);
                kPadded.write(0x00);

                byte[] negkPaddedBytes = negkPadded.toByteArray();
                checkMinimallyEncoded(negkPaddedBytes, negk);

                int last = negkPaddedBytes[negkPaddedBytes.length - 1] & 0x7f;
                negkPadded.reset();
                negkPadded.write(negkPaddedBytes, 0, negkPaddedBytes.length - 1);
                negkPadded.write(last);
                negkPadded.write(0x80);
            }

        } catch (IOException e) {
            //catching UnsafeByteArrayOutputStream.write() should never happen
            throw new RuntimeException(e);
        }
    }

    /** BIN2NUM **/

    public void checkBin2NumOp(byte[] n, byte[] expected) {

    }

}
