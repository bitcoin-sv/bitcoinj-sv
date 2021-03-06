/*
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

package org.bitcoinj.moved.core;

import com.google.common.collect.ImmutableList;
import io.bitcoinsv.bitcoinjsv.blockchain.AbstractBlockChain;
import io.bitcoinsv.bitcoinjsv.core.Address;
import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.core.ECKey;
import org.bitcoinj.moved.testing.TestWithWallet;
import io.bitcoinsv.bitcoinjsv.msg.protocol.Transaction;
import io.bitcoinsv.bitcoinjsv.msg.protocol.TransactionOutput;
import io.bitcoinsv.bitcoinjsv.msg.protocol.TxHelper;
import io.bitcoinsv.bitcoinjsv.params.MainNetParams;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.script.Script;
import io.bitcoinsv.bitcoinjsv.script.ScriptBuilder;
import io.bitcoinsv.bitcoinjsv.temp.SendRequest;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionOutputTest extends TestWithWallet {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testMultiSigOutputToString() throws Exception {
        sendMoneyToWallet(AbstractBlockChain.NewBlockType.BEST_CHAIN, Coin.COIN);
        ECKey myKey = new ECKey();
        this.wallet.importKey(myKey);

        // Simulate another signatory
        ECKey otherKey = new ECKey();

        // Create multi-sig transaction
        Transaction multiSigTransaction = new Transaction(NET);
        ImmutableList<ECKey> keys = ImmutableList.of(myKey, otherKey);

        Script scriptPubKey = ScriptBuilder.createMultiSigOutputScript(2, keys);
        multiSigTransaction.addOutput(Coin.COIN, scriptPubKey);

        SendRequest req = SendRequest.forTx(multiSigTransaction);
        this.wallet.completeTx(req);
        TransactionOutput multiSigTransactionOutput = multiSigTransaction.getOutput(0);

        assertThat(multiSigTransactionOutput.toString(), CoreMatchers.containsString("CHECKMULTISIG"));
    }

    @Test
    public void testP2SHOutputScript() throws Exception {
        String P2SHAddressString = "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU";
        Address P2SHAddress = Address.fromBase58(MainNetParams.get(), P2SHAddressString);
        Script script = ScriptBuilder.createOutputScript(P2SHAddress);
        Transaction tx = new Transaction(Net.MAINNET);
        tx.addOutput(Coin.COIN, script);
        assertEquals(P2SHAddressString, TxHelper.getAddressFromP2SH(tx.getOutput(0), MainNetParams.get()).toString());
    }

    @Test
    public void getAddressTests() throws Exception {
        Transaction tx = new Transaction(Net.MAINNET);
        tx.addOutput(Coin.CENT, ScriptBuilder.createOpReturnScript("hello world!".getBytes()));
        assertNull(TxHelper.getAddressFromP2SH(tx.getOutput(0), PARAMS));
        assertNull(TxHelper.getAddressFromP2PKHScript(tx.getOutput(0), PARAMS));
    }

    @Test
    public void getMinNonDustValue() throws Exception {
        TransactionOutput payToAddressOutput = new TransactionOutput(Net.MAINNET, null, Coin.COIN, myAddress);
        assertEquals(Coin.MIN_NONDUST_OUTPUT, payToAddressOutput.getMinNonDustValue());
    }
}
