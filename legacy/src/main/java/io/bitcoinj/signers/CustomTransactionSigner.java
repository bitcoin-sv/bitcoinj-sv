/*
 * Copyright 2014 Kosta Korenkov
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

package io.bitcoinj.signers;

import io.bitcoinj.core.*;
import io.bitcoinj.crypto.ChildNumber;
import io.bitcoinj.ecc.TransactionSignature;
import io.bitcoinj.ecc.ECDSASignature;
import io.bitcoinj.msg.protocol.Transaction;
import io.bitcoinj.msg.protocol.TransactionInput;
import io.bitcoinj.msg.protocol.TransactionOutput;
import io.bitcoinj.msg.protocol.TxHelper;
import io.bitcoinj.script.Script;
import io.bitcoinj.script.ScriptUtils;
import io.bitcoinj.script.SigHash;
import io.bitcoinj.script.interpreter.ScriptExecutionException;
import io.bitcoinj.script.ScriptUtils_legacy;
import io.bitcoinj.temp.KeyBag;
import io.bitcoinj.temp.RedeemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>This signer may be used as a template for creating custom multisig transaction signers.</p>
 * <p>
 * Concrete implementations have to implement {@link #getSignature(io.bitcoinj.core.Sha256Hash, java.util.List)}
 * method returning a signature and a public key of the keypair used to created that signature.
 * It's up to custom implementation where to locate signatures: it may be a network connection,
 * some local API or something else.
 * </p>
 */
public abstract class CustomTransactionSigner extends StatelessTransactionSigner {
    private static final Logger log = LoggerFactory.getLogger(CustomTransactionSigner.class);

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean signInputs(ProposedTransaction propTx, KeyBag keyBag) {
        Transaction tx = propTx.partialTx;
        int numInputs = tx.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = tx.getInput(i);
            TransactionOutput txOut = txIn.getConnectedOutput();
            if (txOut == null) {
                continue;
            }
            Script scriptPubKey = txOut.getScriptPubKey();
            if (!scriptPubKey.isPayToScriptHash()) {
                log.warn("CustomTransactionSigner works only with P2SH transactions");
                return false;
            }

            Script inputScript = checkNotNull(txIn.getScriptSig());

            try {
                // We assume if its already signed, its hopefully got a SIGHASH type that will not invalidate when
                // we sign missing pieces (to check this would require either assuming any signatures are signing
                // standard output types or a way to get processed signatures out of script execution)
                ScriptUtils_legacy.correctlySpends(txIn.getScriptSig(), tx, i, txIn.getConnectedOutput().getScriptPubKey());
                log.warn("Input {} already correctly spends output, assuming SIGHASH type used will be safe and skipping signing.", i);
                continue;
            } catch (ScriptExecutionException e) {
                // Expected.
            }

            RedeemData redeemData = TxHelper.getConnectedRedeemData(txIn, keyBag);
            if (redeemData == null) {
                log.warn("No redeem data found for input {}", i);
                continue;
            }

            Sha256Hash sighash = propTx.useForkId ?
                    tx.hashForForkIdSignature(i, redeemData.redeemScript, tx.getInput(i).getConnectedOutput().getValue(), SigHash.Flags.ALL, false) :
                    Transaction.hashForLegacySignature(tx, i, redeemData.redeemScript, SigHash.Flags.ALL, false);
            SignatureAndKey sigKey = getSignature(sighash, propTx.keyPaths.get(scriptPubKey));
            TransactionSignature txSig = new TransactionSignature(sigKey.sig, SigHash.Flags.ALL, false, propTx.useForkId);
            int sigIndex = ScriptUtils.getSigInsertionIndex(inputScript, sighash, sigKey.pubKey.getPubKey());
            inputScript = scriptPubKey.getScriptSigWithSignature(inputScript, txSig.encodeToBitcoin(), sigIndex);
            txIn.setScriptSig(inputScript);
        }
        return true;
    }

    protected abstract SignatureAndKey getSignature(Sha256Hash sighash, List<ChildNumber> derivationPath);

    public class SignatureAndKey {
        public final ECDSASignature sig;
        public final ECKey pubKey;

        public SignatureAndKey(ECDSASignature sig, ECKey pubKey) {
            this.sig = sig;
            this.pubKey = pubKey;
        }
    }

}


