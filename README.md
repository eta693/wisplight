# Wisplight — Feux Follets

Mod Minecraft (Java 1.21.1, NeoForge) : des feux follets errent la nuit. Capture-les, récolte leurs essences, infuse-les dans ton équipement… et affronte le Veilleur Éteint.

- Cahier des charges : voir le document partagé.
- Compilation : `./gradlew build` (Java 21). Le `.jar` est dans `build/libs/`.
- Tests en jeu : `./gradlew runGameTestServer`.
- Les assets (textures, sons, JSON) sont générés par les scripts de `tools/`.

Chaque push sur `main` compile le mod et lance les GameTests (GitHub Actions). Le rapport est publié sur la branche `ci-report`.
