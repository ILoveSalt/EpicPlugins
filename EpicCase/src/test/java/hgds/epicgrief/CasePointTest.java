package hgds.epicgrief;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasePointTest {
    @TempDir
    Path temporaryFolder;

    @Test
    void preservesEffectorsParametersAndCopiedSettings() throws IOException {
        Path sourceFile = temporaryFolder.resolve("source.yml");
        Files.writeString(sourceFile, """
                id: source
                location:
                  world: world
                  x: 1
                  y: 2
                  z: 3
                selector: BOOK
                available: [example]
                showOnlyAvailable: true
                effectorKit: [flame, lightning]
                params:
                  power: 4
                  enabled: true
                hologram:
                  type: normal
                  height: 2.5
                  lines:
                    - 't:&aCases'
                """);

        CasePoint source = CasePoint.fromFile(sourceFile.toFile());
        Path savedFile = temporaryFolder.resolve("saved.yml");
        source.save(savedFile.toFile());
        CasePoint saved = CasePoint.fromFile(savedFile.toFile());

        assertEquals("BOOK", saved.getSelector());
        assertEquals(2, saved.getEffectorKit().size());
        assertEquals(4, saved.getParameters().get("power"));
        assertTrue((Boolean) saved.getParameters().get("enabled"));
        assertEquals(source.copySettings(), saved.copySettings());
    }
}
