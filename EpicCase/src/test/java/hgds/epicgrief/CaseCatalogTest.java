package hgds.epicgrief;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseCatalogTest {
    @TempDir
    Path temporaryFolder;

    @Test
    void loadsTreasureCompatibleBoxesAndKeys() throws IOException {
        Path boxes = Files.createDirectories(temporaryFolder.resolve("boxes"));
        Path keys = Files.createDirectories(temporaryFolder.resolve("keys"));
        Files.writeString(boxes.resolve("example.yml"), """
                options:
                  name: '&6Example'
                  item: CHEST
                  animator: none
                  useKeys: true
                  withdrawalAmount: 2
                awards:
                  common:
                    chance: 10
                    rare: true
                    phantom: false
                    skip-permission: epiccase.skip.common
                    hologramText: '&aCommon'
                    hologramItem: DIAMOND
                    actions:
                      - message -t won
                """);
        Files.writeString(keys.resolve("gold.yml"), """
                name: '&6Gold key'
                item: TRIPWIRE_HOOK
                description:
                  - opens example
                canOpen:
                  - example
                """);

        CaseCatalog catalog = new CaseCatalog();
        catalog.reload(boxes.toFile(), keys.toFile(), Logger.getAnonymousLogger());

        CaseBox box = catalog.boxes().get("example");
        CaseKey key = catalog.keys().get("gold");
        assertEquals(2, box.getKeyWithdrawalAmount());
        assertTrue(box.getAwards().getFirst().isRare());
        assertEquals("epiccase.skip.common", box.getAwards().getFirst().getSkipPermission());
        assertTrue(key.canOpen(box.getId()));
        assertEquals(1, catalog.keysFor(box).size());
        assertTrue(catalog.loadErrors().isEmpty());
    }

    @Test
    void loadsEveryBundledCaseConfiguration() {
        CaseCatalog catalog = new CaseCatalog();
        catalog.reload(
                Path.of("src/main/resources/boxes").toFile(),
                Path.of("src/main/resources/keys").toFile(),
                Logger.getAnonymousLogger()
        );

        assertEquals(10, catalog.boxes().size());
        assertEquals(1, catalog.keys().size());
        assertTrue(catalog.loadErrors().isEmpty());
        assertTrue(catalog.boxes().values().stream().allMatch(box -> !box.getAwards().isEmpty()));
    }
}
