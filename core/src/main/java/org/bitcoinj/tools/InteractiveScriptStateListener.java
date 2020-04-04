package org.bitcoinj.tools;

import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bitcoinj.core.Utils.HEX;

/**
 *A simple demonstration of ScriptStateListener that dumps the state of the script interpreter to console after each op code execution.
 *
 * Created by shadders on 7/02/18.
 */
public class InteractiveScriptStateListener extends ScriptStateListener {

    private String fullScriptString;
    private boolean pauseForUser = true;

    public static void main(String[] args) {


        NetworkParameters params = MainNetParams.get();

        // https://blockchain.info/tx/ed27cf72886af7c830faeff136b3859185310334330a4856f60c768ab46b9c1c
        String rawTx1 = "010000000193e3073ecc1d27f17e3d287ccefdfdba5f7d8c160242dbcd547b18baef12f9b31a0000006b483045022100af501dc9ef2907247d28a5169b8362ca494e1993f833928b77264e604329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2fa5d34940180121034bb555cc39ba30561793cf39a35c403fe8cf4a89403b02b51e058960520bd1e3ffffffff02b3bb0200000000001976a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac98100700000000001976a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac00000000";
        // https://blockchain.info/tx/0024db8e11da76b2344e0722bf9488ba2aed611913f9803a62ac3b41f5603946
        String rawtx2 = "01000000011c9c6bb48a760cf656480a33340331859185b336f1effa30c8f76a8872cf27ed000000006a47304402201c999cf44dc6576783c0f55b8ff836a1e22db87ed67dc3c39515a6676cfb58e902200b4a925f9c8d6895beed841db135051f8664ab349f2e3ea9f8523a6f47f93883012102e58d7b931b5d43780fda0abc50cfd568fcc26fb7da6a71591a43ac8e0738b9a4ffffffff029b010100000000001976a9140f0fcdf818c0c88df6860c85c9cc248b9f37eaff88ac95300100000000001976a9140663d2403f560f8d053a25fbea618eb47071617688ac00000000";


        byte[] tx1Bytes = HEX.decode(rawTx1);
        Transaction tx1 = new Transaction(params, tx1Bytes);

        byte[] tx2Bytes = HEX.decode(rawtx2);
        Transaction tx2 = new Transaction(params, tx2Bytes);

        Script scriptPubKey = tx1.getOutput(0).getScriptPubKey();
        Script scriptSig = tx2.getInput(0).getScriptSig();

        LinkedList<byte[]> stack = new LinkedList();

        ScriptStateListener listener = new InteractiveScriptStateListener(true);

        Script script = null;

        System.out.println("\n***Executing scriptSig***\n");
        script = scriptSig;
        Script.executeDebugScript(null, 0, script, stack, Coin.ZERO, Script.ALL_VERIFY_FLAGS, listener);

        System.out.println("\n***Executing scriptPubKey***\n");
        script = scriptPubKey;
        Script.executeDebugScript(tx1, 0, script, stack, Coin.ZERO, Script.ALL_VERIFY_FLAGS, listener);

//        TextScriptParser parser = new TextScriptParser(false, null);
//        parser.addVariable("barry", "0x00112233");
//        script = parser.parse("<barry> 2 add 4 sub");
//        Script.executeDebugScript(tx1, 0, script, stack, Coin.ZERO, Script.ALL_VERIFY_FLAGS, listener);

    }

    public InteractiveScriptStateListener() {
        this(false);
    }

    public InteractiveScriptStateListener(boolean pauseForUser) {
        this.pauseForUser = pauseForUser;
    }

    @Override
    public void onBeforeOpCodeExecuted(boolean willExecute) {

        if (getChunkIndex() == 0) {
            fullScriptString = truncateData(String.valueOf(getScript()));
            System.out.println(fullScriptString);
        }

        System.out.println(String.format("\nExecuting %s operation: [%s]", getCurrentChunk().isOpCode() ? "OP_CODE" : "PUSHDATA", ScriptOpCodes.getOpCodeName(getCurrentChunk().opcode)));
    }

    @Override
    public void onAfterOpCodeExectuted() {

        ScriptBuilder builder = new ScriptBuilder();

        for (ScriptChunk chunk: getScriptChunks().subList(getChunkIndex(), getScriptChunks().size())) {
            builder.addChunk(chunk);
        }

        Script remainScript = builder.build();
        String remainingString = truncateData(remainScript.toString());
        int startIndex = fullScriptString.indexOf(remainingString);
        String markedScriptString = fullScriptString.substring(0, startIndex) + "^" + fullScriptString.substring(startIndex);
        //System.out.println("Remaining code: " + remainingString);
        System.out.println("Execution point (^): " + markedScriptString);
        System.out.println();

        //dump stacks
        List<byte[]> reverseStack = new ArrayList<byte[]>(getStack());
        Collections.reverse(reverseStack);
        System.out.println("Stack:");

        if (reverseStack.isEmpty()) {
          System.out.println("empty");
        } else {
            int index = 0;
            for (byte[] bytes : reverseStack) {

                System.out.println(String.format("index[%s] len[%s] [%s]", index++, bytes.length, HEX.encode(bytes)));

            }
        }
        System.out.println();

        if (!getAltstack().isEmpty()) {
            reverseStack = new ArrayList<byte[]>(getAltstack());
            Collections.reverse(reverseStack);
            System.out.println("Alt Stack:");

            for (byte[] bytes: reverseStack) {
                System.out.println(HEX.encode(bytes));
            }
            System.out.println();
        }

        if (!getIfStack().isEmpty()) {
            List<Boolean>reverseIfStack = new ArrayList<Boolean>(getIfStack());
            Collections.reverse(reverseIfStack);
            System.out.println("If Stack:");

            for (Boolean element: reverseIfStack) {
                System.out.println(element);
            }
            System.out.println();
        }

        if (pauseForUser) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Press enter key to continue");
            scanner.nextLine();
        }

    }

    @Override
    public void onExceptionThrown(ScriptException exception) {
        System.out.println("Exception thrown: ");
    }

    @Override
    public void onScriptComplete() {
        List<byte[]> stack = getStack();
        if (stack.isEmpty() || !Script.castToBool(stack.get(stack.size() - 1))) {
            System.out.println("Script failed.");
        } else {
            System.out.println("Script success.");
        }
    }

    private String truncateData(String scriptString) {

        Pattern p = Pattern.compile("\\[(.*?)\\]");
        Matcher m = p.matcher(scriptString);

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String data = m.group(0);
            if (data.length() > 10) {
                data = data.substring(0, 5) + "..." + data.substring(data.length() - 5);
            }
            m.appendReplacement(sb, data);
        }
        m.appendTail(sb);

        return sb.toString();
    }
}
