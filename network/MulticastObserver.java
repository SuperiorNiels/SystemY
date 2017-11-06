package network;

import java.util.Observable;


public class MulticastObserver extends Observable {
    /**
     * Clears the internal changed state
     */
    @Override
    protected synchronized void clearChanged() {
        super.clearChanged();
    }

    /**
     * Sets the internal changed state, to indicate a change,
     * when notify is called the oberserver will receive the update
     */
    @Override
    public synchronized void setChanged() {
        super.setChanged();
    }
}
