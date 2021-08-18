/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.base;

import io.bitcoinsv.bitcoinjsv.core.BitcoinJ;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

import java.math.BigInteger;
import java.util.Locale;

public interface HeaderReadOnly<C extends BitcoinObject> extends Hashable<C> {

    public static final int FIXED_MESSAGE_SIZE = 80;

    AbstractBlock getBlock();

    long getVersion();

    Sha256Hash getPrevBlockHash();

    Sha256Hash getMerkleRoot();

    long getTime();

    long getDifficultyTarget();

    long getNonce();

    default int fixedSize() {
        return FIXED_MESSAGE_SIZE;
    }

    /**
     * Returns the difficulty target as a 256 bit value that can be compared to a SHA-256 hash. Inside a block the
     * target is represented using a compact form. If this form decodes to a value that is out of bounds, an exception
     * is thrown.
     */
    default BigInteger getDifficultyTargetAsInteger() throws VerificationException {
        BigInteger target = Utils.decodeCompactBits(getDifficultyTarget());
        return target;
    }

    /**
     * Returns the work represented by this block.<p>
     *
     * Work is defined as the number of tries needed to solve a block in the
     * average case. Consider a difficulty target that covers 5% of all possible
     * hash values. Then the work of the block will be 20. As the target gets
     * lower, the amount of work goes up.
     */
    default BigInteger getWork() throws VerificationException {
        BigInteger target = getDifficultyTargetAsInteger();
        return BitcoinJ.LARGEST_HASH.divide(target.add(BigInteger.ONE));
    }

    /**
     * Checks the block data to ensure it follows the rules laid out in the network parameters. Specifically,
     * throws an exception if the proof of work is invalid, or if the timestamp is too far from what it should be.
     * This is <b>not</b> everything that is required for a block to be valid, only what is checkable independent
     * of the chain and without a transaction index.
     *
     * @throws VerificationException
     * @param net
     */
    default void verifyHeader(Net net) throws VerificationException {
        // Prove that this block is OK. It might seem that we can just ignore most of these checks given that the
        // network is also verifying the blocks, but we cannot as it'd open us to a variety of obscure attacks.
        //
        // Firstly we need to ensure this block does in fact represent real work done. If the difficulty is high
        // enough, it's probably been done by the network.
        checkProofOfWork(net.params(), true);
        checkTimestamp();
    }

    /**
     * Returns true if the hash of the block is OK (lower than difficulty target).
     */
    default boolean checkProofOfWork(NetworkParameters params, boolean throwException) throws VerificationException {
        // This part is key - it is what proves the block was as difficult to make as it claims
        // to be. Note however that in the context of this function, the block can claim to be
        // as difficult as it wants to be .... if somebody was able to take control of our network
        // connection and fork us onto a different chain, they could send us valid blocks with
        // ridiculously easy difficulty and this function would accept them.
        //
        // To prevent this attack from being possible, elsewhere we check that the difficultyTarget
        // field is of the right value. This requires us to have the preceeding blocks.
        BigInteger target = getDifficultyTargetAsInteger();
        if (target.signum() <= 0 || target.compareTo(params.getMaxTarget()) > 0) {
            if (throwException)
                throw new VerificationException("Difficulty target is bad: " + target.toString());
            else
                return false;
        }

        BigInteger hash_ = getHash().toBigInteger();
        if (hash_.compareTo(target) > 0) {
            // Proof of work check failed!
            if (throwException) {
                throw new VerificationException("Hash is higher than target: " + getHashAsString() + " vs "
                        + target.toString(16));
            } else
                return false;
        }
        return true;
    }

    /**
     * check the time of this header isn't more than 2 hours into the future.  This is checked when headers are first
     * seen.  A header cannot be considered valid until this threshold is reached meaning a header that is invalid
     * now may become valid later.  However, new block are likely to be found in the meantime rendering this header
     * orphaned.
     * @throws VerificationException
     */
    default void checkTimestamp() throws VerificationException {
        // Allow injection of a fake clock to allow unit testing.
        long currentTime = Utils.currentTimeSeconds();
        if (getTime() > currentTime + BitcoinJ.ALLOWED_TIME_DRIFT)
            throw new VerificationException(String.format(Locale.US, "Block too far in future: %d vs %d", getTime(), currentTime + BitcoinJ.ALLOWED_TIME_DRIFT));
    }
}
