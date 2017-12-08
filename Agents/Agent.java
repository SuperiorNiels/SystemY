package Agents;

import java.io.Serializable;

public abstract class Agent implements Serializable, Runnable {
    public AgentType getType() {
        return type;
    }

    public void setType(AgentType type) {
        this.type = type;
    }

    private AgentType type;
    public void start() { this.start(); }
}
