package br.com.rapidmesck.rapidbolao;

import java.util.UUID;

public class Bet {
    private UUID playerId;
    private double amount;

    public Bet(UUID playerId, double amount) {
        this.playerId = playerId;
        this.amount = amount;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getAmount() {
        return amount;
    }
}
