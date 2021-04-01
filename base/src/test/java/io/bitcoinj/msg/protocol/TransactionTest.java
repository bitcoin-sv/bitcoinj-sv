/*
 * Copyright 2014 Google Inc.
 * Copyright 2016 Andreas Schildbach
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

package io.bitcoinj.msg.protocol;

import io.bitcoinj.bitcoin.Genesis;
import io.bitcoinj.bitcoin.api.base.*;
import io.bitcoinj.bitcoin.bean.base.TxBean;
import io.bitcoinj.bitcoin.bean.base.TxInputBean;
import io.bitcoinj.bitcoin.bean.base.TxOutPointBean;
import io.bitcoinj.bitcoin.bean.validator.TxBeanValidator;
import io.bitcoinj.blockchain.SPVBlockChain;
import io.bitcoinj.core.*;
import io.bitcoinj.exception.BlockStoreException;
import io.bitcoinj.exception.VerificationException;
import io.bitcoinj.params.*;
import io.bitcoinj.script.*;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.bitcoinj.core.Utils.HEX;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Just check the Transaction.verify() method. Most methods that have complicated logic in Transaction are tested
 * elsewhere, e.g. signing and hashing are well exercised by the wallet tests, the full block chain tests and so on.
 * The verify method is also exercised by the full block chain tests, but it can also be used by API users alone,
 * so we make sure to cover it here as well.
 */
public class TransactionTest {
    private static final NetworkParameters PARAMS = UnitTestParams.get();
    private static final Net NET = Net.UNITTEST;
    private static final AddressLite ADDRESS = new ECKeyLite().toAddress(PARAMS);

    private Tx tx;

    @BeforeEach
    public void setUp() throws Exception {
        byte[] bytes = HEX.decode("01000000013df681ff83b43b6585fa32dd0e12b0b502e6481e04ee52ff0fdaf55a16a4ef61000000006b483045022100a84acca7906c13c5895a1314c165d33621cdcf8696145080895cbf301119b7cf0220730ff511106aa0e0a8570ff00ee57d7a6f24e30f592a10cae1deffac9e13b990012102b8d567bcd6328fd48a429f9cf4b315b859a58fd28c5088ef3cb1d98125fc4e8dffffffff02364f1c00000000001976a91439a02793b418de8ec748dd75382656453dc99bcb88ac40420f000000000017a9145780b80be32e117f675d6e0ada13ba799bf248e98700000000");
        tx = new TxBean(bytes).makeMutable();
    }

    @Test
    public void emptyOutputs() {
        assertThrows(VerificationException.EmptyInputsOrOutputs.class, () -> {
            tx.setOutputs(Collections.emptyList());
            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });
    }

    @Test
    public void emptyInputs() {
        assertThrows(VerificationException.EmptyInputsOrOutputs.class, () -> {
            tx.setInputs(Collections.emptyList());
            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });
    }

    @Test
    public void t(){
        byte[] bytes = new BigInteger("60434553797637094804667845667193334646959818742304445166416869027409503646167051384327544423344685365256542521959461542140779362019783834503431169315418541327979706904888607680988280357701735671814950326322039706589326306401759536981357018042841446447750331416566403008987127051581873120957283664850519588729047841225070438855367273").toByteArray();
        String hex = IntStream.range(0, bytes.length)
                .map(i -> bytes[i] & 0xff)
                .mapToObj(b -> String.format("%02x", b))
                .collect(Collectors.joining());

        byte[] bytes2 = new BigInteger("744843883781622873690649129936361905246114178054369360775340").toByteArray();
        String hex2 = IntStream.range(0, bytes2.length)
                .map(i -> bytes2[i] & 0xff)
                .mapToObj(b -> String.format("%02x", b))
                .collect(Collectors.joining());

        byte[] bytes3= new BigInteger("12194330279919401087825177399983979926362021121934921705042875775651641449417382247668545073912082607781846389688037375704637601413970677202944").toByteArray();
        String hex3 = IntStream.range(0, bytes3.length)
                .map(i -> bytes3[i] & 0xff)
                .mapToObj(b -> String.format("%02x", b))
                .collect(Collectors.joining());

        System.out.println(hex);
        System.out.println(hex2);
        System.out.println(hex3);

    }

    //@Test
    public void tooHugeToSerialize() {
        assertThrows(ProtocolException.class, () -> {
            tx.getInputs().get(0).setScriptBytes(new byte [TxParams.MAX_TRANSACTION_SIZE_PARAM * 2]);

            new TxBean(tx.serialize());
        });
    }

    @Test
    public void duplicateOutPoint() {
        assertThrows(VerificationException.DuplicatedOutPoint.class, () -> {
            TxInput txInput = tx.getInputs().get(0).mutableCopy();
            txInput.setScriptBytes(new byte[1]);

            tx.getInputs().add(txInput);

            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });

    }

    @Test
    public void negativeOutput() {
        assertThrows(VerificationException.NegativeValueOutput.class, () -> {
            tx.getOutputs().get(0).setValue(Coin.NEGATIVE_SATOSHI);
            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });
    }

    @Test
    public void exceedsMaxMoney2() {
        assertThrows(VerificationException.ExcessiveValue.class, () -> {
            Coin half = PARAMS.getMaxMoney().add(Coin.SATOSHI);

            tx.getOutputs().get(0).setValue(half);

            tx.getOutputs().add(tx.getOutputs().get(0).mutableCopy());

            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });
    }

    @Test
    public void coinbaseInputInNonCoinbaseTX() {
        assertThrows(VerificationException.UnexpectedCoinbaseInput.class, () -> {

            TxInput txInput = new TxInputBean(tx);
            txInput.setScriptSig(new ScriptBuilder().data(new byte[10]).build());

            TxOutPoint txOutPoint = new TxOutPointBean(txInput);
            txOutPoint.setHash(Sha256Hash.ZERO_HASH);
            txOutPoint.setIndex(0xFFFFFFFFL);

            txInput.setOutpoint(txOutPoint);

            tx.getInputs().add(txInput);

            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });
    }

    @Test
    public void coinbaseScriptSigTooSmall() {
        assertThrows(VerificationException.CoinbaseScriptSizeOutOfRange.class, () -> {

            TxInput txInput = new TxInputBean(tx);
            txInput.setScriptSig(new ScriptBuilder().build());

            TxOutPoint txOutPoint = new TxOutPointBean(txInput);
            txOutPoint.setHash(Sha256Hash.ZERO_HASH);
            txOutPoint.setIndex(0xFFFFFFFFL);


            txInput.setOutpoint(txOutPoint);

            tx.setInputs(Arrays.asList(txInput));

            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });
    }

    @Test
    public void coinbaseScriptSigTooLarge()  {
        assertThrows(VerificationException.CoinbaseScriptSizeOutOfRange.class, () -> {

            TxInput txInput = new TxInputBean(tx);
            txInput.setScriptSig(new ScriptBuilder().data(new byte[99]).build());
            TxOutPoint txOutPoint = new TxOutPointBean(txInput);

            txOutPoint.setHash(Sha256Hash.ZERO_HASH);
            txOutPoint.setIndex(0xFFFFFFFFL);

            txInput.setOutpoint(txOutPoint);

            tx.setInputs(Arrays.asList(txInput));

            assertEquals(101, tx.getInputs().get(0).getScriptBytes().length);

            new TxBeanValidator(PARAMS.getNet().params()).validate(tx);
        });
    }

    @Test
    public void testEstimatedLockTime_WhenParameterSignifiesBlockHeight() throws IOException, BlockStoreException {
        int TEST_LOCK_TIME = 1;
        Date now = Calendar.getInstance().getTime();

        File file = File.createTempFile("spvblockstore", null);
        file.delete();
        file.deleteOnExit();

        SPVBlockChain mockSPVBlockChain = createMock(SPVBlockChain.class);
        EasyMock.expect(mockSPVBlockChain.estimateBlockTime(TEST_LOCK_TIME)).andReturn(now);

        tx.setLockTime(TEST_LOCK_TIME); // less than five hundred million

        replay(mockSPVBlockChain);

        //assertEquals(tx.estimateLockTime(mockSPVBlockChain), now);
    }

//    @Test //TODO
//    public void testOptimalEncodingMessageSize() {
//        Transaction tx = new Transaction(NET);
//
//        int length = tx.length();
//
//        // add basic transaction input, check the length
//        tx.addOutput(new TransactionOutput(NET, null, Coin.COIN, ADDRESS));
//        length += getCombinedLength(tx.getOutputs());
//
//        // add basic output, check the length
//        length += getCombinedLength(tx.getInputs());
//
//        // optimal encoding size should equal the length we just calculated
//        assertEquals(tx.getOptimalEncodingMessageSize(), length);
//    }


//    @Test TODO
//    public void testIsMatureReturnsFalseIfTransactionIsCoinbaseAndConfidenceTypeIsNotEqualToBuilding() {
//        Transaction tx = FakeTxBuilder.createFakeCoinbaseTx(NET);
//
//        TxHelper.getConfidence(tx).setConfidenceType(ConfidenceType.UNKNOWN);
//        assertEquals(tx.isMature(), false);
//
//        TxHelper.getConfidence(tx).setConfidenceType(ConfidenceType.PENDING);
//        assertEquals(tx.isMature(), false);
//
//        TxHelper.getConfidence(tx).setConfidenceType(ConfidenceType.DEAD);
//        assertEquals(tx.isMature(), false);
//    }
//
//    @Test TODO
//    public void testCLTVPaymentChannelTransactionSpending() {
//        BigInteger time = BigInteger.valueOf(20);
//
//        ECKey from = new ECKey(), to = new ECKey(), incorrect = new ECKey();
//        Script outputScript = ScriptBuilder.createCLTVPaymentChannelOutput(time, from, to);
//
//        Transaction tx = new Transaction(NET);
//        tx.addInput(new TransactionInput(NET, tx, new byte[] {}));
//        tx.getInput(0).setSequenceNumber(0);
//        tx.setLockTime(time.subtract(BigInteger.ONE).longValue());
//        TransactionSignature fromSig =
//                tx.calculateLegacySignature(0, from, outputScript, SigHash.Flags.SINGLE, false);
//        TransactionSignature toSig =
//                tx.calculateLegacySignature(0, to, outputScript, SigHash.Flags.SINGLE, false);
//        TransactionSignature incorrectSig =
//                tx.calculateLegacySignature(0, incorrect, outputScript, SigHash.Flags.SINGLE, false);
//        Script scriptSig =
//                ScriptBuilder.createCLTVPaymentChannelInput(fromSig, toSig);
//        Script refundSig =
//                ScriptBuilder.createCLTVPaymentChannelRefund(fromSig);
//        Script invalidScriptSig1 =
//                ScriptBuilder.createCLTVPaymentChannelInput(fromSig, incorrectSig);
//        Script invalidScriptSig2 =
//                ScriptBuilder.createCLTVPaymentChannelInput(incorrectSig, toSig);
//
//        try {
//            ScriptUtils_legacy.correctlySpends(scriptSig, tx, 0, outputScript, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
//        } catch (ScriptExecutionException e) {
//            e.printStackTrace();
//            fail("Settle transaction failed to correctly spend the payment channel");
//        }
//
//        try {
//            ScriptUtils_legacy.correctlySpends(refundSig, tx, 0, outputScript, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
//            fail("Refund passed before expiry");
//        } catch (ScriptExecutionException e) { }
//        try {
//            ScriptUtils_legacy.correctlySpends(invalidScriptSig1, tx, 0, outputScript, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
//            fail("Invalid sig 1 passed");
//        } catch (ScriptExecutionException e) { }
//        try {
//            ScriptUtils_legacy.correctlySpends(invalidScriptSig2, tx, 0, outputScript, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
//            fail("Invalid sig 2 passed");
//        } catch (ScriptExecutionException e) { }
//    }
//
//    @Test TODO
//    public void testCLTVPaymentChannelTransactionRefund() {
//        BigInteger time = BigInteger.valueOf(20);
//
//        ECKey from = new ECKey(), to = new ECKey(), incorrect = new ECKey();
//        Script outputScript = ScriptBuilder.createCLTVPaymentChannelOutput(time, from, to);
//
//        Transaction tx = new Transaction(NET);
//        tx.addInput(new TransactionInput(NET, tx, new byte[] {}));
//        tx.getInput(0).setSequenceNumber(0);
//        tx.setLockTime(time.add(BigInteger.ONE).longValue());
//        TransactionSignature fromSig =
//                tx.calculateLegacySignature(0, from, outputScript, SigHash.Flags.SINGLE, false);
//        TransactionSignature incorrectSig =
//                tx.calculateLegacySignature(0, incorrect, outputScript, SigHash.Flags.SINGLE, false);
//        Script scriptSig =
//                ScriptBuilder.createCLTVPaymentChannelRefund(fromSig);
//        Script invalidScriptSig =
//                ScriptBuilder.createCLTVPaymentChannelRefund(incorrectSig);
//
//        try {
//            ScriptUtils_legacy.correctlySpends(scriptSig, tx, 0, outputScript, ScriptVerifyFlag.ALL_VERIFY_FLAGS_PRE_GENESIS);
//        } catch (ScriptExecutionException e) {
//            e.printStackTrace();
//            fail("Refund failed to correctly spend the payment channel");
//        }
//
//        try {
//            ScriptUtils_legacy.correctlySpends(invalidScriptSig, tx, 0, outputScript, ScriptVerifyFlag.ALL_VERIFY_FLAGS);
//            fail("Invalid sig passed");
//        } catch (ScriptExecutionException e) { }
//    }
//
//    @Test TODO
//    public void testToStringWhenLockTimeIsSpecifiedInBlockHeight() {
//
//        tx.getInputs().get(0).setSequenceNumber(42);
//
//        int TEST_LOCK_TIME = 20;
//        tx.setLockTime(TEST_LOCK_TIME);
//
//        Calendar cal = Calendar.getInstance();
//        cal.set(2085, 10, 4, 17, 53, 21);
//        cal.set(Calendar.MILLISECOND, 0);
//
//        SPVBlockChain mockSPVBlockChain = createMock(SPVBlockChain.class);
//        EasyMock.expect(mockSPVBlockChain.estimateBlockTime(TEST_LOCK_TIME)).andReturn(cal.getTime());
//
//        replay(mockSPVBlockChain);
//
//        String str = tx.toString();
//
//        assertEquals(str.contains("block " + TEST_LOCK_TIME), true);
//        //assertEquals(str.contains("estimated to be reached at"), true);
//    }
//
//    @Test TODO
//    public void testToStringWhenIteratingOverAnInputCatchesAnException() {
//        Transaction tx = FakeTxBuilder.createFakeTx(NET);
//        TransactionInput ti = new TransactionInput(NET, tx, new byte[0]) {
//            @Override
//            public Script getScriptSig() throws ScriptExecutionException {
//                throw new ScriptExecutionException("");
//            }
//        };
//
//        tx.addInput(ti);
//        assertEquals(tx.toString().contains("[exception: "), true);
//    }
//
//    @Test
//    public void testToStringWhenThereAreZeroInputs() {
//        Transaction tx = new Transaction(NET);
//        assertEquals(tx.toString().contains("No inputs!"), true);
//    }
//
//    @Test TODO
//    @SuppressWarnings("SelfEquals")     // todo: consider using http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html
//    public void testTheTXByHeightComparator() {
//        Transaction tx1 = FakeTxBuilder.createFakeTx(NET);
//        TxHelper.getConfidence(tx1).setAppearedAtChainHeight(1);
//
//        Transaction tx2 = FakeTxBuilder.createFakeTx(NET);
//        TxHelper.getConfidence(tx2).setAppearedAtChainHeight(2);
//
//        Transaction tx3 = FakeTxBuilder.createFakeTx(NET);
//        TxHelper.getConfidence(tx3).setAppearedAtChainHeight(3);
//
//        SortedSet<Transaction> set = new TreeSet<Transaction>(Transaction.SORT_TX_BY_HEIGHT);
//        set.add(tx2);
//        set.add(tx1);
//        set.add(tx3);
//
//        Iterator<Transaction> iterator = set.iterator();
//
//        assertEquals(tx1.equals(tx2), false);
//        assertEquals(tx1.equals(tx3), false);
//        assertEquals(tx1.equals(tx1), true);
//
//        assertEquals(iterator.next().equals(tx3), true);
//        assertEquals(iterator.next().equals(tx2), true);
//        assertEquals(iterator.next().equals(tx1), true);
//        assertEquals(iterator.hasNext(), false);
//    }
//
//    @Test(expected = ScriptExecutionException.class) TODO
//    public void testAddSignedInputThrowsExceptionWhenScriptIsNotToRawPubKeyAndIsNotToAddress() {
//        ECKeyLite key = new ECKeyLite();
//        AddressLite addr = key.toAddress(PARAMS);
//        Transaction fakeTx = FakeTxBuilder.createFakeTx(NET, Coin.COIN, addr);
//
//        Transaction tx = new Transaction(NET);
//        tx.addOutput(fakeTx.getOutput(0));
//
//        Script script = ScriptBuilder.createOpReturnScript(new byte[0]);
//
//        TxHelper.addSignedInput(tx, fakeTx.getOutput(0).getOutPointFor(), script, key);
//    }
//
//    @Test TODO
//    public void testPrioSizeCalc() throws Exception {
//        Tx tx1 = FakeTxBuilder.createFakeTx(NET, Coin.COIN, ADDRESS);
//        int size1 = tx1.getMessageSize();
//        int size2 = tx1.getMessageSizeForPriorityCalc();
//        assertEquals(113, size1 - size2);
//        tx1.getInput(0).setScriptSig(new Script(new byte[109]));
//        assertEquals(78, tx1.getMessageSizeForPriorityCalc());
//        tx1.getInput(0).setScriptSig(new Script(new byte[110]));
//        assertEquals(78, tx1.getMessageSizeForPriorityCalc());
//        tx1.getInput(0).setScriptSig(new Script(new byte[111]));
//        assertEquals(79, tx1.getMessageSizeForPriorityCalc());
//    }
//
//    @Test TODO
//    public void testCoinbaseHeightCheck() throws VerificationException {
//        // Coinbase transaction from block 300,000
//        final byte[] transactionBytes = HEX.decode("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4803e09304062f503253482f0403c86d53087ceca141295a00002e522cfabe6d6d7561cf262313da1144026c8f7a43e3899c44f6145f39a36507d36679a8b7006104000000000000000000000001c8704095000000001976a91480ad90d403581fa3bf46086a91b2d9d4125db6c188ac00000000");
//        final int height = 300000;
//        final Transaction transaction = Serializer.defaultFor(NET).makeTransaction(transactionBytes);
//        transaction.checkCoinBaseHeight(height);
//    }
//
//    /**
//     * Test a coinbase transaction whose script has nonsense after the block height.
//     * See https://github.com/bitcoinj/bitcoinj/issues/1097
//     */
//    @Test TODO
//    public void testCoinbaseHeightCheckWithDamagedScript() throws VerificationException {
//        // Coinbase transaction from block 224,430
//        final byte[] transactionBytes = HEX.decode(
//            "010000000100000000000000000000000000000000000000000000000000000000"
//            + "00000000ffffffff3b03ae6c0300044bd7031a0400000000522cfabe6d6d0000"
//            + "0000000000b7b8bf0100000068692066726f6d20706f6f6c7365727665726aac"
//            + "1eeeed88ffffffff01e0587597000000001976a91421c0d001728b3feaf11551"
//            + "5b7c135e779e9f442f88ac00000000");
//        final int height = 224430;
//        final Transaction transaction = Serializer.defaultFor(NET).makeTransaction(transactionBytes);
//        transaction.checkCoinBaseHeight(height);
//    }
//
//    @Test TODO
//    public void optInFullRBF() {
//        // a standard transaction as wallets would create
//        Transaction tx = FakeTxBuilder.createFakeTx(NET);
//        assertFalse(tx.isOptInFullRBF());
//
//        tx.getInputs().get(0).setSequenceNumber(TransactionInput.NO_SEQUENCE - 2);
//        assertTrue(tx.isOptInFullRBF());
//    }
//
//    /**
//     * Ensure that hashForSignature() doesn't modify a transaction's data, which could wreak multithreading havoc.
//     */
    @Test
    @SuppressWarnings("UnusedAnonymousClass")       // todo: check https://errorprone.info/bugpattern/UnusedAnonymousClass
    public void testHashForSignatureThreadSafety() {
        FullBlock genesis = Genesis.getFor(PARAMS.getNet());


        final Tx tx = genesis.getTransactions().get(0);
        final String txHash = tx.getHashAsString();
        final String txNormalizedHash = SigHash.hashForLegacySignature(tx, 0, new byte[0], SigHash.Flags.ALL.byteValue()).toString();

        for (int i = 0; i < 100; i++) {
            // ensure the transaction object itself was not modified; if it was, the hash will change
            assertEquals(txHash, tx.getHashAsString());
            new Thread(){
                public void run() {
                    assertEquals(txNormalizedHash, SigHash.hashForLegacySignature(tx, 0, new byte[0], SigHash.Flags.ALL.byteValue()).toString());
                }
            };
        }
    }

    @Test
    public void testHashForSignature()
    {
        MainNetParams MAIN = new MainNetParams(Net.MAINNET);
        String dumpedPrivateKey = "KyYyHLChvJKrM4kxCEpdmqR2usQoET2V1JbexZjaxV36wytPw7v1";
        DumpedPrivateKeyLite dumpedPrivateKey1 = DumpedPrivateKeyLite.fromBase58(MAIN, dumpedPrivateKey);
        ECKeyLite key = dumpedPrivateKey1.getKey();

        String txData = "0200000001411d29708a0b4165910fbc73b6efbd3d183b1bf457d8840beb23874714c41f61010000006a47304402204b3b868a9a966c44fb05f2cfb3c888b5617435d00ebe1dfe4bd452fd538592d90220626adfb79def08c0375de226b77cefbd3c659aad299dfe950539d01d2770132a41210354662c29cec7074ad26af8664bffdb7f540990ece13a872da5fdfa8be019563efeffffff027f5a1100000000001976a914dcbfe1b282c167c1942a2bdc927de8b4a368146588ac400d0300000000001976a914fb57314db46dd11b4a99c16779a5e160858df43888acd74f0700";
        String txConnectedData = "020000000284ff1fbdee5aeeaf7976ddfb395e00066c150d4ed90da089f5b47e46215dc23c010000006b4830450221008e1f85698b5130f2dd56236541f2b2c1f7676721acebbbdc3c8711a345d2f96b022065f1f2ea915b8844319b3e81e33cb6a26ecee838dc0060248b10039e994ab1e641210248dd879c54147390a12f8e8a7aa8f23ce2659a996fa7bf756d6b2187d8ed624ffeffffffefd0db693d73d8087eb1f44916be55ee025f25d7a3dbcf82e3318e56e6ccded9000000006a4730440221009c6ba90ca215ce7ad270e6688940aa6d97be6c901a430969d9d88bef7c8dc607021f51d088dadcaffbd88e5514afedfa9e2cac61a1024aaa4c88873361193e4da24121039cc4a69e1e93ebadab2870c69cb4feb0c1c2bfad38be81dda2a72c57d8b14e11feffffff0230c80700000000001976a914517abefd39e71c633bd5a23fd75b5dbd47bc461b88acc8911400000000001976a9147b983c4efaf519e9caebde067b6495e5dcc491cb88acba4f0700";
        Tx txConnected = new TxBean(HEX.decode(txConnectedData));
        Tx tx = new TxBean(HEX.decode(txData));

        Script sig = tx.getInputs().get(0).getScriptSig();

        ScriptUtils.correctlySpends(sig, tx, 0, txConnected.getOutputs().get(1).getScriptPubKey(), txConnected.getOutputs().get(1).getValue(), ScriptVerifyFlag.ALL_VERIFY_FLAGS);


    }
}
