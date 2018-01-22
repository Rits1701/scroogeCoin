import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class TxHandler {
    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise. (the difference can be thought of as transaction fee)
     */
    public boolean isValidTx(Transaction tx) {

        // Step 1, 2, 3
        double sumInput = 0;
        Set<UTXO> utxoSet = new HashSet<UTXO>();
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO tmp = new UTXO(input.prevTxHash, input.outputIndex);

            if (utxoSet.contains(tmp))
                return false;

            utxoSet.add(tmp);
            if (pool.contains(tmp)) {
                Transaction.Output output = pool.getTxOutput(tmp);

                if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                    return false;
                }
                sumInput += output.value;
            } else {
                return false;
            }
        }

        // Step 4
        double sumOutput = 0;
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output output = tx.getOutput(i);
            if (output.value >= 0) {
                sumOutput += output.value;
            } else {
                return false;
            }
        }

        // Step 5
        return sumInput >= sumOutput;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> transactions = new ArrayList<Transaction>();
        for (int i = 0; i < possibleTxs.length; i++) {
            transactions.add(possibleTxs[i]);
        }

        boolean anyTransactionValid = false;
        ArrayList<Transaction> finalList = new ArrayList<Transaction>();
        do {
            anyTransactionValid = false;
            Transaction validTx = null;
            for (Transaction tx: transactions) {
                if (isValidTx(tx)) {
                    validTx = tx;
                    anyTransactionValid = true;

                    // Update UTXO pool
                    for (Transaction.Input input: tx.getInputs()) {
                        pool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                    }

                    for (int i = 0; i < tx.numOutputs(); i++) {
                        pool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
                    }
                    break;
                }
            }

            if (validTx != null) {
                finalList.add(validTx);
                transactions.remove(validTx);
            }
        } while (anyTransactionValid && transactions.size() > 0);

        Transaction[] results = new Transaction[finalList.size()];
        results = finalList.toArray(results);
        return results;
    }

}
