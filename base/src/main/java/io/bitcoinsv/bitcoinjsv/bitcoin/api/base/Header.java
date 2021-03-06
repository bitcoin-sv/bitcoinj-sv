/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.bitcoin.api.base;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.exception.VerificationException;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.BitcoinObject;
import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

import java.util.Date;

public interface Header<C extends Header> extends BitcoinObject<C>, HeaderReadOnly<C> {

    void setHash(Sha256Hash hash);

    void setVersion(long version);

    void setPrevBlockHash(Sha256Hash prevBlockHash);

    void setMerkleRoot(Sha256Hash merkleRoot);

    void setTime(long time);

    void setDifficultyTarget(long difficultyTarget);

    void setNonce(long nonce);

    /**
     * <p>Finds a value of nonce that makes the blocks hash lower than the difficulty target. This is called mining, but
     * solve() is far too slow to do real mining with. It exists only for unit testing purposes.
     *
     * <p>This can loop forever if a solution cannot be found solely by incrementing nonce. It doesn't change
     * extraNonce.</p>
     *
     * Note this will modify the nonce so can only be called on a mutable object.
     */
    default void solve(Net net) {
        NetworkParameters params = net.params();
        while (true) {
            try {
                // Is our proof of work valid yet?
                if (checkProofOfWork(params, false))
                    return;
                // No, so increment the nonce and try again.
                setNonce(getNonce() + 1);
            } catch (VerificationException e) {
                throw new RuntimeException(e); // Cannot happen.
            }
        }
    }

    /**
     * Copy header values from another header object
     * @param other
     */
    default void copyFrom(HeaderReadOnly other) {
        setVersion(other.getVersion());
        setPrevBlockHash(other.getPrevBlockHash());
        setMerkleRoot(other.getMerkleRoot());
        setTime(other.getTime());
        setDifficultyTarget(other.getDifficultyTarget());
        setNonce(other.getNonce());
    }

    default Date getTimeAsDate() {
        return new Date(getTime() * 1000);
    }
}
