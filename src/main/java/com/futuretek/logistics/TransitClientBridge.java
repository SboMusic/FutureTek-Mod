package com.futuretek.logistics;

/**
 * Ponte verso il client per l'animazione dei tubi: il codice del client (TransitRenderer) viene
 * chiamato solo quando il pacchetto arriva su un client, quindi il server dedicato non lo carica mai.
 */
public final class TransitClientBridge {
    private TransitClientBridge() {
    }

    public static void receive(ItemTransit.Payload payload) {
        com.futuretek.client.TransitRenderer.add(payload);
    }
}
