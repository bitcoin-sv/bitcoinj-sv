/*
 * Copyright 2014 Andreas Schildbach
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

package io.bitcoinj.script;

import static io.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA1;
import static io.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA2;
import static io.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA4;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.bitcoinj.script.interpreter.Interpreter;
import org.junit.jupiter.api.Test;

public class ScriptChunkTest {

    @Test
    public void testShortestPossibleDataPush() {
        assertTrue(new ScriptBuilder().data(new byte[0]).build().getChunks().get(0)
                .isShortestPossiblePushData(), "empty push");

        for (byte i = -1; i < 127; i++)
            assertTrue(new ScriptBuilder().data(new byte[] { i }).build().getChunks()
                    .get(0).isShortestPossiblePushData(), "push of single byte " + i);

        //This limit is enough to get us into the range of PUSHDATA4
        long limit = Short.MAX_VALUE * 2 + Interpreter.MAX_SCRIPT_ELEMENT_SIZE;
        for (int len = 2; len < limit; len++)
            assertTrue(new ScriptBuilder().data(new byte[len]).build().getChunks().get(0)
                    .isShortestPossiblePushData(), "push of " + len + " bytes");

        // non-standard chunks
        for (byte i = 1; i <= 16; i++)
            assertFalse(new ScriptChunk(1, new byte[] { i }).isShortestPossiblePushData(), "push of smallnum " + i);
        assertFalse(new ScriptChunk(OP_PUSHDATA1, new byte[75]).isShortestPossiblePushData(), "push of 75 bytes");
        assertFalse(new ScriptChunk(OP_PUSHDATA2, new byte[255]).isShortestPossiblePushData(), "push of 255 bytes");
        assertFalse(new ScriptChunk(OP_PUSHDATA4, new byte[65535]).isShortestPossiblePushData(), "push of 65535 bytes");
    }
}
