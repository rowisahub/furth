package com.rowisabeast.futurehit.playerClass;

import org.bson.Document;

import java.util.UUID;

public class playerClass {

    public Document PlayerDBLocal;

    public playerClass(
            UUID UUID,
            String Username,
            int Lives,
            int numberOfTimesPlayerHasKilledBountys,
            int numberOfTImesPlayerHasDiedAsBountys,
            boolean ifKilledByBountys,
            String connectionInformationHosts,
            int connectionInformationPorts,
            boolean isOnlines,
            boolean isBountys,
            boolean isNextBountys,
            String deadBodyLocationWorlds,
            double deadBodyLocationXs,
            double deadBodyLocationYs,
            double deadBodyLocationzs,
            String SkinTextures,
            String SkinSignatures,
            boolean isAlives
    ){

        PlayerDBLocal = new Document()
                .append("_id", UUID)
                .append("uuid", UUID)
                .append("username", Username)
                .append("lives", Lives)
                .append("numberOfTimesPlayerHasKilledBounty", numberOfTimesPlayerHasKilledBountys)
                .append("numberOfTImesPlayerHasDiedAsBounty", numberOfTImesPlayerHasDiedAsBountys)
                .append("ifKilledByBounty", ifKilledByBountys)
                .append("connectionInformationHost", connectionInformationHosts)
                .append("connectionInformationPort", connectionInformationPorts)
                .append("isOnline", isOnlines)
                .append("isBounty", isBountys)
                .append("isNextBounty", isNextBountys)
                .append("deadBodyLocationWorld", deadBodyLocationWorlds)
                .append("deadBodyLocationX", deadBodyLocationXs)
                .append("deadBodyLocationY", deadBodyLocationYs)
                .append("deadBodyLocationz", deadBodyLocationzs)
                .append("SkinTexture", SkinTextures)
                .append("SkinSignature", SkinSignatures)
                .append("isAlive", isAlives);
        // set Document as public and edit that

    }
}
/*
playerClass pc = new playerClass(
                    uuid,
                    pl.getName(),
                    (int)Db.get("lives"),
                    (int)Db.get("numberOfTimesPlayerHasKilledBounty"),
                    (int)Db.get("numberOfTImesPlayerHasDiedAsBounty"),
                    (boolean)Db.get("ifKilledByBounty"),
                    (String)Db.get("connectionInformationHost"),
                    (String)Db.get("connectionInformationPort"),
                    (boolean)Db.get("isOnline"),
                    (boolean)Db.get("isBounty"),
                    (boolean)Db.get("isNextBounty"),
                    (String)Db.get("deadBodyLocationWorld"),
                    (double)Db.get("deadBodyLocationX"),
                    (double)Db.get("deadBodyLocationY"),
                    (double)Db.get("deadBodyLocationz"),
                    (String)Db.get("SkinTexture"),
                    (String)Db.get("SkinSignature"),
                    (boolean)Db.get("isAlive")
            );
            playerDBLocal.put(uuid, pc);
 */
