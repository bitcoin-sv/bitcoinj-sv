package org.bitcoinj.msg;

import javax.annotation.Nullable;

public interface IChildMessage extends IMessage {
    @Nullable
    <M extends IMessage> M getParent();
}
