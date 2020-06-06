package org.bitcoinj.core.listeners;

import org.bitcoinj.msg.EmptyMessage;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.Net;

/**
 * Created by HashEngineering on 8/11/2017.
 */
public class SendHeadersMessage extends EmptyMessage{
    public SendHeadersMessage(Net net){
        super(net);
    }
}
