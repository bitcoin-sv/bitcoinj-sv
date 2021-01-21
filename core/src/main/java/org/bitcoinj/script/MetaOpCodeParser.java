package org.bitcoinj.script;

import org.bitcoinj.core.VarInt;

public class MetaOpCodeParser {

    private byte[] program;
    private int offset;

    public static void parseProgram(byte[] bytes, int offset,
                                    int txOffset, //offset of the begining of the tx in bytes
                                    int offsetInTx //offset of the beginning of of the meta opcode with respect to the beginning of the tx
    ) {
        int initialOffset = offset;

        int metaOpcode = bytes[offset];
        offset++;

        // Length of replaced data in total
        VarInt dataLenLong = new VarInt(bytes, offset);
        int dataLen = (int) dataLenLong.value;
        offset += dataLenLong.getOriginalSizeInBytes();

        /**
         * In order to pass the Sha256 midstate we need the replaced data to finish on a 64 byte boundary with respect to the entire serialized transaction. Since we are replacing some data
         * with the content of the resolved meta-opcode we may need up to 63 bytes of the inital part of the missing data to complete a 64 byte block and complete the 64 byte block.
         */
        VarInt padBytesStartLen = new VarInt(bytes, offset);
        offset += padBytesStartLen.getOriginalSizeInBytes();
        byte[] padBytesStart = new byte[(int) padBytesStartLen.value];
        System.arraycopy(bytes, offset, padBytesStart, 0, (int) padBytesStartLen.value);
        offset += padBytesStartLen.value;

        // the hash from begining of tx to end of the meta-data chunk + any additional bytes required to get to end of block boundary
        byte[] hashEndState = new byte[32];
        System.arraycopy(bytes, offset, hashEndState, 0, 32);

        /**
         * verify data chunk:
         *
         * initalise the SHA256 function with the hash of the beginning of the tx up to end of padBytesStart
         * Sha256 Hash the data chunk + any tx data up to the next block boundary
         * verify the hash matches hashEndState
         */

        /**
         * verify tx:
         * same as above but hash the remainder of bytes to complete the tx hash then hash the hash to get txid.
         */



    }

}
