package application.network.message;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final MessageType type;
    private final Object payload;

    public NetworkMessage(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return String.format("NetworkMessage{type=%s, payload=%s}", 
            type, 
            payload != null ? payload.toString() : "null"
        );
    }
}