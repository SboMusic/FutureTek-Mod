package com.futuretek.logistics;

import java.util.List;

/** Ultimo contenuto della rete ricevuto dal server (usato solo dalla grafica del terminale sul client). */
public final class TerminalClientData {
    public static volatile TerminalPayloads.Contents contents = new TerminalPayloads.Contents(List.of(), List.of());

    private TerminalClientData() {
    }
}
