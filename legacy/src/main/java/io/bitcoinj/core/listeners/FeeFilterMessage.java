package io.bitcoinj.core.listeners;

import io.bitcoinj.msg.EmptyMessage;
import io.bitcoinj.params.Net;

/**
 * Created by HashEngineering on 8/11/2017.
 */
public class FeeFilterMessage extends EmptyMessage{
    public FeeFilterMessage(Net net){
        super(net);
    }
}
