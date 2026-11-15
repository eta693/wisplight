# Wisplight — Feux Follets

Mod Minecraft **Java 1.21.1 / NeoForge** : la nuit, des feux follets errent dans le monde. Capture-les, récolte leurs essences, infuse-les dans ton équipement… et ravive la flamme du Veilleur Éteint.

## Installation

1. Installe **NeoForge 1.21.1** (21.1.266 ou plus récent) avec ton launcher (Prism Launcher, CurseForge, Modrinth App…).
2. Télécharge `wisplight-x.y.z.jar` depuis la page [Releases](https://github.com/eta693/wisplight/releases).
3. Dépose le fichier dans le dossier `mods` de ton instance, puis lance le jeu.

Aucune autre dépendance. Compatible serveur dédié (le mod doit être installé des deux côtés).

## Contenu

| Domaine | Contenu |
| --- | --- |
| Créatures | 4 feux follets (Azur, Émeraude, Braise, Givre), Ombre rampante, boss **le Veilleur Éteint** (3 phases) |
| Ressource | Lumen : minerai, éclat, lingot, bloc ; outils et armure entre fer et diamant |
| Bocaux | Capture accroupi, production d'essences (5 min), récolte à la fiole, comparateur |
| Infusion | Autel avec interface, 16 effets (armes, outils, armures, bâton), 4 charmes |
| Équipement | Bâton des follets, Armure du Veilleur, Bâton du Veilleur, Lanterne éternelle |
| Monde | Minerai de lumen, apparitions par biome, **Sanctuaire oublié** avec crypte |
| Divers | Boussole des lueurs, lanterne d'appel, 4 lanternes de lumen, grimoire, 15 succès, 29 sons, FR + EN |

Toutes les recettes d'infusion sont des fichiers JSON (`data/wisplight/recipe/infusion/`, type `wisplight:infusion`) modifiables par datapack.

## Configuration

`config/wisplight-common.toml` : lumière dynamique des follets, nombre maximum de follets par zone, durée de production d'une essence.

## Développement

```bash
./gradlew build              # compile le mod → build/libs/
./gradlew runGameTestServer  # 31 tests automatiques en jeu
./gradlew runClient          # lance le jeu de développement
```

Les assets sont générés par code (`tools/`) :

| Script | Produit |
| --- | --- |
| `gen_textures.py` | toutes les textures pixel art (124) |
| `gen_assets.py` | modèles, états de blocs, recettes, loot tables, tags, génération du monde, succès, traductions |
| `gen_sounds.py` | les 29 sons, synthétisés puis encodés en Ogg Vorbis |
| `gen_structures.py` | modèles NBT : arènes de test et Sanctuaire oublié |
| `check_assets.py` | vérification de cohérence (textures, modèles, traductions, sons) |

Chaque push sur `main` lance la CI : vérification des assets, compilation, GameTests, puis une visite automatique en jeu avec captures d'écran (branches `ci-report` et `ci-tour`).

## Licence

MIT — code et assets créés pour ce mod.
