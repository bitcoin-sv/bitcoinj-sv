/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.params;


import java.util.Date;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class STNParams extends TestNet3Params {

    public static final String ID_GBTN = "org.bitcoin.stn";

    public STNParams(Net net) {
        super(net);
        id = ID_GBTN;
        packetMagic = 0xfbcec4f9L;
        port = 9333;
        dnsSeeds = new String[] {
                "stn-seed.bitcoinsv.io"
        };
        uahfHeight = 16; //this value is 1 higher than in chainparams.cpp
        daaUpdateHeight = 2200; //Must be greater than 2016 - chainparams.cpp
    }

    private static STNParams instance = new STNParams(Net.STN);
    public static synchronized STNParams get() {
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return "stn";
    }

    // February 16th 2012
    private static final Date testnetDiffDate = new Date(1329264000000L);

    public static boolean isValidTestnetDateBlock(Date blockTime){
        return blockTime.after(testnetDiffDate);
    }

}

