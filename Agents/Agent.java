package Agents;

import java.io.Serializable;

public abstract class Agent implements Serializable, Runnable {

    private AgentType type;

    public Agent(AgentType type) {
        setType(type);
    }

    public AgentType getType() {
        return type;
    }

    public void setType(AgentType type) {
        this.type = type;
    }
}
