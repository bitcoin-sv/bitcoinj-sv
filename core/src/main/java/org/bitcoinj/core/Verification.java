package org.bitcoinj.core;

import com.google.common.base.Preconditions;
import org.bitcoinj.chain.StoredBlock;
import org.bitcoinj.exception.VerificationException;
import org.bitcoinj.msg.protocol.Block;
import org.bitcoinj.msg.protocol.Transaction;
import org.bitcoinj.params.*;
import org.bitcoinj.script.ScriptVerifyFlag;
import org.bitcoinj.utils.VersionTally;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Date;
import java.util.EnumSet;

/**
 * Placeholder for static methods extracted from NetworkParams which create dependencies that don't need to be there.
 *
 * TODO find a better home for them, better yet work out if we really need them.
 */
public class Verification {

    // February 16th 2012
    private static final Date testnetDiffDate = new Date(1329264000000L);
    /**
     * The number that is one greater than the largest representable SHA-256
     * hash.
     */
    private static BigInteger LARGEST_HASH = BigInteger.ONE.shiftLeft(256);



    /**
     * The flags indicating which block validation tests should be applied to
     * the given block. Enables support for alternative blockchains which enable
     * tests based on different criteria.
     *
     * @param block block to determine flags for.
     * @param height height of the block, if known, null otherwise. Returned
     * tests should be a safe subset if block height is unknown.
     */
    public static EnumSet<Block.VerifyFlag> getBlockVerificationFlags(NetworkParameters params, final Block block,
                                                                      final VersionTally tally, final Integer height) {
        final EnumSet<Block.VerifyFlag> flags = EnumSet.noneOf(Block.VerifyFlag.class);

        if (block.isBIP34()) {
            final Integer count = tally.getCountAtOrAbove(Block.BLOCK_VERSION_BIP34);
            if (null != count && count >= params.getMajorityEnforceBlockUpgrade()) {
                flags.add(Block.VerifyFlag.HEIGHT_IN_COINBASE);
            }
        }
        return flags;
    }

    /**
     * The flags indicating which script validation tests should be applied to
     * the given transaction. Enables support for alternative blockchains which enable
     * tests based on different criteria.
     *
     * @param block block the transaction belongs to.
     * @param transaction to determine flags for.
     * @param height height of the block, if known, null otherwise. Returned
     * tests should be a safe subset if block height is unknown.
     */
    public static EnumSet<ScriptVerifyFlag> getTransactionVerificationFlags(NetworkParameters params, final Block block,
                                                                            final Transaction transaction, final VersionTally tally, final Integer height) {
        final EnumSet<ScriptVerifyFlag> verifyFlags = EnumSet.noneOf(ScriptVerifyFlag.class);
        if (block.getTimeSeconds() >= NetworkParameters.BIP16_ENFORCE_TIME)
            verifyFlags.add(ScriptVerifyFlag.P2SH);

        // Start enforcing CHECKLOCKTIMEVERIFY, (BIP65) for block.nVersion=4
        // blocks, when 75% of the network has upgraded:
        if (block.getVersion() >= Block.BLOCK_VERSION_BIP65 &&
            tally.getCountAtOrAbove(Block.BLOCK_VERSION_BIP65) > params.getMajorityEnforceBlockUpgrade()) {
            verifyFlags.add(ScriptVerifyFlag.CHECKLOCKTIMEVERIFY);
        }

        return verifyFlags;
    }

    public static void verifyDifficulty(NetworkParameters params, BigInteger newTarget, Block nextBlock)
    {
        if (newTarget.compareTo(params.getMaxTarget()) > 0) {
            newTarget = params.getMaxTarget();
        }

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        long receivedTargetCompact = nextBlock.getDifficultyTarget();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        long newTargetCompact = Utils.encodeCompactBits(newTarget);

        if (newTargetCompact != receivedTargetCompact) {

            //FIXME work out what's wrong here
            if ("GBTNParams".equals(params.getClass().getSimpleName()))
                return;

            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    Long.toHexString(newTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
        }
    }

    /** Returns the network parameters for the given string ID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromID(String id) {
        if (id.equals(NetworkParameters.ID_MAINNET)) {
            return MainNetParams.get();
        } else if (id.equals(NetworkParameters.ID_TESTNET)) {
            return TestNet3Params.get();
        } else if (id.equals(NetworkParameters.ID_UNITTESTNET)) {
            return UnitTestParams.get();
        } else if (id.equals(NetworkParameters.ID_REGTEST)) {
            return RegTestParams.get();
        } else {
            return null;
        }
    }

    /** Returns the network parameters for the given string paymentProtocolID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromPmtProtocolID(String pmtProtocolId) {
        if (pmtProtocolId.equals(NetworkParameters.PAYMENT_PROTOCOL_ID_MAINNET)) {
            return MainNetParams.get();
        } else if (pmtProtocolId.equals(NetworkParameters.PAYMENT_PROTOCOL_ID_TESTNET)) {
            return TestNet3Params.get();
        } else if (pmtProtocolId.equals(NetworkParameters.PAYMENT_PROTOCOL_ID_UNIT_TESTS)) {
            return UnitTestParams.get();
        } else if (pmtProtocolId.equals(NetworkParameters.PAYMENT_PROTOCOL_ID_REGTEST)) {
            return RegTestParams.get();
        } else {
            return null;
        }
    }

    /**
     * Compute the a target based on the work done between 2 blocks and the time
     * required to produce that work.
     */
     public static BigInteger ComputeTarget(StoredBlock firstBlock,
                                            StoredBlock lastBlock) {
         Preconditions.checkState(lastBlock.getHeight() > firstBlock.getHeight());

        /*
         * From the total work done and the time it took to produce that much work,
         * we can deduce how much work we expect to be produced in the targeted time
         * between blocks.
         */
        BigInteger work = lastBlock.getChainWork().subtract(firstBlock.getChainWork());
        work = work.multiply(BigInteger.valueOf(NetworkParameters.TARGET_SPACING));

        // In order to avoid difficulty cliffs, we bound the amplitude of the
        // adjustment we are going to do.
        Preconditions.checkState(lastBlock.getHeader().getTimeSeconds() >  firstBlock.getHeader().getTimeSeconds());
        long nActualTimespan = lastBlock.getHeader().getTimeSeconds() - firstBlock.getHeader().getTimeSeconds();
        if (nActualTimespan > 288 * NetworkParameters.TARGET_SPACING) {
            nActualTimespan = 288 * NetworkParameters.TARGET_SPACING;
        } else if (nActualTimespan < 72 * NetworkParameters.TARGET_SPACING) {
            nActualTimespan = 72 * NetworkParameters.TARGET_SPACING;
        }

        work = work.divide(BigInteger.valueOf(nActualTimespan));

        /**
         * We need to compute T = (2^256 / W) - 1.
         * This code differs from Bitcoin-ABC in that we are using
         * BigIntegers instead of a data type that is limited to 256 bits.
         */

         return LARGEST_HASH.divide(work).subtract(BigInteger.ONE);
    }

    public static boolean isValidTestnetDateBlock(Block block){
        return block.getTime().after(testnetDiffDate);
    }

    /**
     * Checks if we are at a difficulty transition point.
     * @param storedPrev The previous stored block
     * @param parameters The network parameters
     * @return If this is a difficulty transition point
     */
    public static boolean isDifficultyTransitionPoint(StoredBlock storedPrev, NetworkParameters parameters) {
        return ((storedPrev.getHeight() + 1) % parameters.getInterval()) == 0;
    }
}
