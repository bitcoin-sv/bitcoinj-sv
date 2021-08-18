package io.bitcoinsv.bitcoinjsv.core.listeners;

import io.bitcoinsv.bitcoinjsv.msg.EmptyMessage;
import io.bitcoinsv.bitcoinjsv.params.Net;

/**
 * Created by HashEngineering on 8/11/2017.
 */
public class FeeFilterMessage extends EmptyMessage {
    public FeeFilterMessage(Net net){
        super(net);
    }
}
